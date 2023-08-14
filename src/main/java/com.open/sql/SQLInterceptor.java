package com.open.sql;


import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.toolkit.TableNameParser;
import com.open.utils.JdbcUtils;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.util.HashSet;

/**
 * @author cfang
 * @date 2023/08/02 14:07
 * @desc
 */
@Slf4j
@Intercepts({
                @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}),
                @Signature(type = StatementHandler.class, method = "getBoundSql", args = {}),
                @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
                @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
                @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
public class SQLInterceptor implements Interceptor {

    @Autowired
    Tracer tracer;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Span span = null;
        Scope scope = null;
        try{
            Object target = invocation.getTarget();
            if (target instanceof Executor) {
                Executor executor = (Executor) target;
                Object[] args = invocation.getArgs();
                Object parameter = args[1];
                MappedStatement ms = (MappedStatement) args[0];
                String sqlCommandType = ms.getSqlCommandType().name();
                /**
                 * 从事务管理上下文中获取 Connection，无需手动 release
                 */
//                String dbName = DataSourceUtils.getConnection(dataSource).getCatalog();
                /**
                 *  参考自 com.baomidou.mybatisplus.extension.toolkit.JdbcUtils#getDbType(org.apache.ibatis.executor.Executor) 中获取 Connection 的方式
                 */
                Connection connection = executor.getTransaction().getConnection();
                String dbName = connection.getCatalog();
                /**
                 * 参考自 com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor#changeTable(java.lang.String)
                 */
                BoundSql boundSql = ms.getBoundSql(parameter);
                HashSet<String> tables = (HashSet<String>) new TableNameParser(boundSql.getSql()).tables();
                String spanName = String.join(" ", sqlCommandType, dbName);
                String tableName = "";
                /**
                 * 单表操作，spanName串上表名，如果多张表，spanName无需串上表名
                 */
                if(tables.size() == 1){
                    tableName = tables.stream().findFirst().get();
                    spanName = String.join(".", spanName, tableName);
                }
                span = tracer.spanBuilder(spanName)
                        .setParent(Context.current().with(Span.current()))
                        .startSpan();
                scope = span.makeCurrent();
                span.setAttribute(SemanticAttributes.DB_STATEMENT, boundSql.getSql());
                span.setAttribute(SemanticAttributes.DB_CONNECTION_STRING, connection.getMetaData().getURL());
                span.setAttribute(SemanticAttributes.DB_USER, connection.getMetaData().getUserName());
                span.setAttribute(SemanticAttributes.DB_OPERATION, sqlCommandType);
                DbType dbType = JdbcUtils.getDbType(connection.getMetaData().getURL());
                span.setAttribute(SemanticAttributes.DB_SYSTEM, dbType.getDb());
                if(StringUtils.hasText(tableName)){
                    span.setAttribute(SemanticAttributes.DB_SQL_TABLE, tableName);
                }
            }
            return invocation.proceed();
        }catch (Exception e){
            if(null != span){
                span.setStatus(StatusCode.ERROR, e.getMessage());
            }
            throw e;
        }finally {
            if(null != scope){
                scope.close();
            }
            if(null != span){
                span.end();
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof Executor || target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

}
