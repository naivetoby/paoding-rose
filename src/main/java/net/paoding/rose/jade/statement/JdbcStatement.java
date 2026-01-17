/*
 * Copyright 2009-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License i distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.paoding.rose.jade.statement;

import net.paoding.rose.jade.annotation.AfterInvocation;
import net.paoding.rose.jade.annotation.ReturnGeneratedKeys;
import net.paoding.rose.jade.annotation.SQLType;
import net.paoding.rose.jade.jpa.JpaDetail;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Pageable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author 王志亮 [qieqie.wang@gmail.com]
 */
public class JdbcStatement implements Statement {

    private static final Log sqlLogger = LogFactory.getLog(JdbcStatement.class);

    private final StatementMetaData metaData;

    private final Interpreter[] interpreters;

    private final Querier querier;

    private final AfterInvocationCallback afterInvocationCallback;

    private final boolean batchUpdate;

    private final SQLType sqlType;

    private final String logPrefix;

    private static final AfterInvocationCallback NULL_AFTER_INVOCATION_CALLBACK = (runtime, returnValue) -> returnValue;

    public JdbcStatement(StatementMetaData statementMetaData, SQLType sqlType, Interpreter[] interpreters, Querier querier) {
        this.metaData = statementMetaData;
        AfterInvocation afterInvocationAnnotation = metaData.getMethod().getAnnotation(AfterInvocation.class);
        if (afterInvocationAnnotation != null) {
            try {
                this.afterInvocationCallback = afterInvocationAnnotation.value().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            this.afterInvocationCallback = NULL_AFTER_INVOCATION_CALLBACK;
        }
        this.interpreters = (interpreters == null) ? new Interpreter[0] : interpreters;
        this.querier = querier;
        this.sqlType = sqlType;
        if (sqlType == SQLType.WRITE) {
            Method method = statementMetaData.getMethod();
            Class<?>[] types = method.getParameterTypes();
            Class<?> returnType = statementMetaData.getReturnType();
            if (returnType.isPrimitive()) {
                returnType = ClassUtils.primitiveToWrapper(returnType);
            }
            if (types.length == 1 && List.class.isAssignableFrom(types[0])) {
                this.batchUpdate = true;
                if (metaData.getMethod().getAnnotation(ReturnGeneratedKeys.class) != null) {
                    // 批量处理的直接不支持@ReturnGeneratedKeys注解
                    throw new InvalidDataAccessApiUsageException(
                            "batch update method cannot return generated keys: " + method);
                }
                if (returnType != void.class && returnType != int[].class && returnType != Integer.class && returnType != Boolean.class) {
                    throw new InvalidDataAccessApiUsageException("error return type, only support type of {void,boolean,int,int[]}: " + method);
                }
            } else {
                this.batchUpdate = false;
                if (returnType != void.class && returnType != Boolean.class
                        && !Number.class.isAssignableFrom(returnType)) {
                    throw new IllegalArgumentException("error return type:"
                            + method.getDeclaringClass().getName() + "#" + method.getName() + "-->"
                            + returnType);
                }
            }
        } else {
            this.batchUpdate = false;
        }
        this.logPrefix = "\n\t@method:" + this.metaData;
    }

    @Override
    public StatementMetaData getMetaData() {
        return metaData;
    }

    @Override
    public Object execute(Map<String, Object> parameters, Object[] args) {
        Object result;
        if (batchUpdate) {
            result = batchUpdate(parameters);
        } else {
            StatementRuntime runtime = new StatementRuntimeImpl(metaData, parameters);
            if (runtime.isAnalysisWithJap()) {
                Optional<Pageable> pageable = Optional.empty();
                if (Objects.nonNull(args)) {
                    pageable = Arrays.stream(args).filter(o -> o instanceof Pageable).map(o -> (Pageable) o).findFirst();
                }
                JpaDetail jpaDetail = metaData.getJpaUtil().methodToSql(args);
                runtime.setArgs(jpaDetail.getArgs().toArray(new Object[0]));
                runtime.setSQL(jpaDetail.sql(pageable));
            } else {
                for (Interpreter interpreter : interpreters) {
                    interpreter.interpret(runtime);
                }
            }
            log(parameters, runtime);
            result = querier.execute(sqlType, batchUpdate, runtime);
            result = afterInvocationCallback.execute(runtime, result);
        }
        if (metaData.getReturnType() == Optional.class && result == null) {
            return Optional.empty();
        }
        return result;

    }

    /**
     * 批量操作
     * @param parameters
     * @return
     */
    private Object batchUpdate(Map<String, Object> parameters) {
        Object result;
        Iterable<?> iterable = (Iterable<?>) parameters.get(":1");
        Iterator<?> iterator = iterable.iterator();
        List<StatementRuntime> runtimes = new LinkedList<>();
        int index = 0;
        while (iterator.hasNext()) {
            Object arg = iterator.next();
            HashMap<String, Object> clone = new HashMap<String, Object>(parameters);
            // 更新执行参数
            clone.put(":1", arg);
            if (metaData.getSQLParamAt(0) != null) {
                clone.put(metaData.getSQLParamAt(0).value(), arg);
            }
            StatementRuntime runtime = new StatementRuntimeImpl(metaData, clone);
            for (Interpreter interpreter : interpreters) {
                interpreter.interpret(runtime);
            }
            if (index == 0) {
                log(parameters, runtime);
            }
            runtimes.add(runtime);
            index++;
        }
        result = querier.execute(sqlType, batchUpdate, runtimes.toArray(new StatementRuntime[0]));
        result = afterInvocationCallback.execute(runtimes.get(0), result);
        return result;
    }

    private void log(Map<String, Object> parameters, StatementRuntime runtime) {
        if (!sqlLogger.isInfoEnabled()) return;

        // 获取原始 SQL 并处理换行
        String cleanSql = runtime.getSQL().replaceAll("\\s+", " ").trim();
        List<Object> args = runtime.getArgs();

        StringBuilder sb = new StringBuilder(512); // 512 足够了
        sb.append(logPrefix);
        sb.append("\n\t#SQL:\t").append(runtime.getMetaData().getSQL().replaceAll("\\s+", " ").trim());
        sb.append("\n\t@sql:\t").append(cleanSql);

        // 参数部分
        sb.append("\n\t@args:\t");
        List<String> keys = new ArrayList<>(parameters.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            sb.append(key).append("='").append(parameters.get(key)).append("'  ");
        }

//        // 拼接完整 SQL 的逻辑
//        // 使用 -1 确保末尾的问号能被正确拆分
//        String[] parts = cleanSql.split("\\?", -1);
//
//        // 严谨判断：零件数必须等于参数数 + 1
//        if (args != null && parts.length == args.size() + 1) {
//            StringBuilder fullSql = new StringBuilder(cleanSql.length() + args.size() * 10);
//            for (int i = 0; i < args.size(); i++) {
//                fullSql.append(parts[i]);
//                Object val = args.get(i);
//                // 简单处理：如果是字符串加引号，null 打印为 NULL
//                if (val instanceof String) {
//                    fullSql.append("'").append(val).append("'");
//                } else {
//                    fullSql.append(val);
//                }
//            }
//            fullSql.append(parts[parts.length - 1]); // 加上最后一段
//            sb.append("\n\tSQL:\t").append(fullSql);
//        }

        sqlLogger.info(sb.toString());
    }

//    private void log(Map<String, Object> parameters, StatementRuntime runtime) {
//        if (sqlLogger.isInfoEnabled()) {
//            StringBuilder sql = new StringBuilder(runtime.getSQL().replaceAll("\\s+", " ").trim());
//            List<Object> args = runtime.getArgs();
//            StringBuilder sb = new StringBuilder(1024);
//            sb.append(logPrefix);
//            sb.append("\n\t#SQL:\t").append(runtime.getMetaData().getSQL().replaceAll("\\s+", " ").trim());
//            sb.append("\n\t@sql:\t").append(sql);
//            sb.append("\n\t@args:\t");
//            ArrayList<String> keys = new ArrayList<String>(parameters.keySet());
//            Collections.sort(keys);
//            for (String key : keys) {
//                sb.append(key).append("='").append(parameters.get(key)).append("'  ");
//            }
//            // sb.append("\n\t$sql:\t").append(runtime.getSQL());
//            String[] splitSql = sql.toString().split("\\?");
//            if (!ObjectUtils.isEmpty(splitSql) && !CollectionUtils.isEmpty(args) && splitSql.length - 1 == args.size()) {
//                sql = new StringBuilder();
//                for (int i = 0; i < args.size(); i++) {
//                    sql.append(splitSql[i]).append("'").append(args.get(i)).append("'");
//                }
//                sql.append(splitSql[splitSql.length-1]);
//                sb.append("\n\tSQL:\t").append(sql.toString().replaceAll("\\s+", " ").trim());
//            }
//            sqlLogger.info(sb.toString());
//        }
//    }

//    private void log(Map<String, Object> parameters, StatementRuntime runtime) {
//        if (sqlLogger.isInfoEnabled()) {
//            String rawSql = runtime.getSQL();
//            // 1. 去掉 SQL 内部的换行符和多余空格，使其变回单行
//            String singleLineSql = rawSql.replaceAll("\\s+", " ").trim();
//
//            StringBuilder sb = new StringBuilder(512);
//            sb.append(logPrefix);
//
//            // 2. 只记录一次带占位符的单行 SQL
//            sb.append("\n\t[SQL]:\t").append(singleLineSql);
//
//            // 3. 记录参数 (保持在一行)
//            sb.append("\n\t[Args]:\t{");
//            List<String> keys = new ArrayList<>(parameters.keySet());
//            Collections.sort(keys);
//            for (int i = 0; i < keys.size(); i++) {
//                String key = keys.get(i);
//                sb.append(key).append("=").append(parameters.get(key));
//                if (i < keys.size() - 1) sb.append(", ");
//            }
//            sb.append("}");
//
//            // 4. (可选) 如果你想看填充后的完整 SQL，可以取消下面注释，同样处理为单行
//            List<Object> args = runtime.getArgs();
//            String[] splitSql = rawSql.split("\\?");
//            if (!ObjectUtils.isEmpty(splitSql) && !CollectionUtils.isEmpty(args) && splitSql.length - 1 == args.size()) {
//                StringBuilder fullSql = new StringBuilder();
//                for (int i = 0; i < args.size(); i++) {
//                    fullSql.append(splitSql[i]).append("'").append(args.get(i)).append("'");
//                }
//                fullSql.append(splitSql[splitSql.length-1]);
//                sb.append("\n\t[FullSQL]: ").append(fullSql.toString().replaceAll("\\s+", " ").trim());
//            }
//
//            sqlLogger.info(sb.toString());
//        }
//    }

}
