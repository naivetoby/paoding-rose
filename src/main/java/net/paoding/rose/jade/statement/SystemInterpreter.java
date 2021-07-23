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

import net.paoding.rose.jade.annotation.condition.SQLCondition;
import net.paoding.rose.jade.statement.expression.ExqlPattern;
import net.paoding.rose.jade.statement.expression.impl.ExqlContextImpl;
import net.paoding.rose.jade.statement.expression.impl.ExqlPatternImpl;
import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.BadSqlGrammarException;

import java.sql.SQLSyntaxErrorException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * @author 廖涵 [in355hz@gmail.com]
 */
public class SystemInterpreter implements Interpreter {

    @Override
    public void interpret(StatementRuntime runtime) {
        try {

            ExqlContextImpl context = new ExqlContextImpl(runtime.getSQL().length() + 32);
            Map<String, Object> parameters = runtime.getParameters();
            Map<String, ?> constants = runtime.getMetaData().getDAOMetaData().getConstants();
            // 转换语句中的表达式
            ExqlPattern pattern = ExqlPatternImpl.compile(runtime.getSQL());
            pattern.execute(context, parameters, constants);
            runtime.setArgs(context.getParams());
            StringBuilder sql = new StringBuilder(context.flushOut());
            // 转换 and 表达式
            SQLCondition[] sqlConditions = runtime.getSQLAnds();
            if (sqlConditions != null && sqlConditions.length > 0) {
                String value, condition;
                String[] values;
                ArrayList<String> valueList;
                ExqlPattern sqlAndPattern;
                ExqlContextImpl sqlAndContext;
                boolean appendWhere = true;
                for (SQLCondition sqlCondition : sqlConditions) {
                    value = sqlCondition.value();
                    condition = sqlCondition.condition();
                    values = sqlCondition.values();
                    valueList = new ArrayList<>(values.length + 1);
                    valueList.add(value);
                    valueList.addAll(Arrays.asList(values));
                    for (String expression : valueList) {
                        if (StringUtils.isNotBlank(expression)) {
                            sqlAndPattern = ExqlPatternImpl.compile(expression);
                            sqlAndContext = new ExqlContextImpl(expression.length() + 32);
                            sqlAndPattern.execute(sqlAndContext, parameters, constants);
                            String sqlConditionStr = sqlAndContext.flushOut();
                            if (StringUtils.isNotBlank(sqlConditionStr)) {
                                if (appendWhere && StringUtils.isNotBlank(condition) && sqlCondition.appendWhere()) {
                                    sql.append(" where ").append(sqlConditionStr);
                                    appendWhere = false;
                                } else {
                                    sql.append(" ").append(condition).append(" ").append(sqlConditionStr);
                                }
                                runtime.setArgs(sqlAndContext.getParams());
                            }
                        }
                    }
                }
            }
            runtime.setSQL(sql.toString());
        } catch (Exception e) {
            String daoInfo = runtime.getMetaData().toString();
            throw new BadSqlGrammarException(daoInfo, runtime.getSQL(),
                    new SQLSyntaxErrorException(daoInfo + " @SQL('" + runtime.getSQL() + "')", e));
        }

    }


}
