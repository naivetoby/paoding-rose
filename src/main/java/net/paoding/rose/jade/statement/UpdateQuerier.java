/*
 * Copyright 2009-2010 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.paoding.rose.jade.annotation.ReturnGeneratedKeys;
import net.paoding.rose.jade.annotation.SQLType;
import net.paoding.rose.jade.dataaccess.DataAccess;
import net.paoding.rose.jade.dataaccess.DataAccessFactory;

import org.apache.commons.lang.ClassUtils;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/**
 * 
 * @author 王志亮 [qieqie.wang@gmail.com]
 * @author 廖涵 [in355hz@gmail.com]
 */
public class UpdateQuerier implements Querier {

    private final DataAccessFactory dataAccessProvider;

    private final Class<?> returnType;

    private final boolean returnGeneratedKeys;

    public UpdateQuerier(DataAccessFactory dataAccessProvider, StatementMetaData metaData) {
        this.dataAccessProvider = dataAccessProvider;
        Method method = metaData.getMethod();
        // 转换基本类型
        Class<?> returnType = method.getReturnType();
        if (returnType.isPrimitive()) {
            returnType = ClassUtils.primitiveToWrapper(returnType);
        }
        this.returnType = returnType;
        returnGeneratedKeys = returnType != void.class && (method.isAnnotationPresent(ReturnGeneratedKeys.class));
    }

    @Override
    public Object execute(SQLType sqlType, StatementRuntime... runtimes) {
        switch (runtimes.length) {
            case 0:
                return 0;
            case 1:
                return executeSingle(runtimes[0], returnType);
            default:
                return executeBatch(runtimes);
        }
    }

    private Object executeSingle(StatementRuntime runtime, Class<?> returnType) {
        Number result;
        DataAccess dataAccess = dataAccessProvider.getDataAccess(//
                runtime.getMetaData(), runtime.getProperties());
        if (returnGeneratedKeys) {
            ArrayList<Map<String, Object>> keys = new ArrayList<Map<String, Object>>(1);
            KeyHolder generatedKeyHolder = new GeneratedKeyHolder(keys);
            dataAccess.update(runtime.getSQL(), runtime.getArgs(), generatedKeyHolder);
            if (keys.size() > 0) {
                result = generatedKeyHolder.getKey();
            } else {
                result = null;
            }
        } else {
            result = dataAccess.update(runtime.getSQL(), runtime.getArgs(), null);
        }
        //
        if (result == null || returnType == void.class) {
            return null;
        }
        if (returnType == result.getClass()) {
            return result;
        }

        // 将结果转成方法的返回类型
        if (returnType == Integer.class) {
            return result.intValue();
        } else if (returnType == Long.class) {
            return result.longValue();
        } else if (returnType == Boolean.class) {
            return result.intValue() > 0 ? Boolean.TRUE : Boolean.FALSE;
        } else if (returnType == Double.class) {
            return result.doubleValue();
        } else if (returnType == Float.class) {
            return result.floatValue();
        } else if (Number.class.isAssignableFrom(returnType)) {
            return result;
        } else {
            throw new DataRetrievalFailureException(
                    "The generated key is not of a supported numeric type. " + "Unable to cast ["
                            + result.getClass().getName() + "] to [" + Number.class.getName() + "]");
        }
    }

    //TODO: 支持returnGeneratedKeys
    private Object executeBatch(StatementRuntime... runtimes) {
        int[] updatedArray = new int[runtimes.length];
        Map<String, List<StatementRuntime>> batchs = new HashMap<String, List<StatementRuntime>>();
        for (int i = 0; i < runtimes.length; i++) {
            StatementRuntime runtime = runtimes[i];
            List<StatementRuntime> batch = batchs.computeIfAbsent(runtime.getSQL(), k -> new LinkedList<StatementRuntime>());
            // 该runtime在batch中的位置
            runtime.setProperty("_index_at_batch_", i);
            batch.add(runtime);
        }
        // TODO: 多个真正的batch可以考虑并行执行~待定
        for (Map.Entry<String, List<StatementRuntime>> batch : batchs.entrySet()) {
            String sql = batch.getKey();
            List<StatementRuntime> batchRuntimes = batch.getValue();
            StatementRuntime runtime = batchRuntimes.get(0);
            DataAccess dataAccess = dataAccessProvider.getDataAccess(
                    runtime.getMetaData(), runtime.getProperties());
            List<Object[]> argsList = new ArrayList<>(batchRuntimes.size());
            for (StatementRuntime batchRuntime : batchRuntimes) {
                argsList.add(batchRuntime.getArgs().parallelStream().toArray());
            }
            int[] batchResult = dataAccess.batchUpdate(sql, argsList);
            if (batchs.size() == 1) {
                updatedArray = batchResult;
            } else {
                int indexAtSubBatch = 0;
                for (StatementRuntime batchRuntime : batchRuntimes) {
                    Integer index_at_batch_ = batchRuntime.getProperty("_index_at_batch_");
                    updatedArray[index_at_batch_] = batchResult[indexAtSubBatch++];
                }
            }
        }
        return updatedArray;
    }

    @SuppressWarnings("unused")
    private Object executeBatch_(StatementRuntime... runtimes) {
        int[] updatedArray = new int[runtimes.length];
        for (int i = 0; i < updatedArray.length; i++) {
            StatementRuntime runtime = runtimes[i];
            updatedArray[i] = (Integer) executeSingle(runtime, Integer.class);
        }
        return updatedArray;
    }

}
