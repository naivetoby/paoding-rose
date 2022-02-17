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

import java.util.*;

/**
 * @author qieqie
 */
public class StatementRuntimeImpl implements StatementRuntime {

    private final StatementMetaData metaData;

    private final Map<String, Object> parameters;

    private String sql;

    private SQLCondition[] sqlCondition;

    private final List<Object> args = new ArrayList<>();

    private Map<String, Object> properties;

    private boolean analysisWithJap;

    public StatementRuntimeImpl(StatementMetaData metaData, Map<String, Object> parameters) {
        this.metaData   = metaData;
        this.parameters = parameters;
        this.sql        = metaData.getSQL();
        SQLCondition[] sqlConditions = metaData.getSqlConditions();
        if (sqlConditions != null && sqlConditions.length > 0) {
            this.sqlCondition = sqlConditions.clone();
        }
        this.analysisWithJap = metaData.isAnalysisWithJap();
    }

    @Override
    public StatementMetaData getMetaData() {
        return metaData;
    }

    @Override
    public Map<String, Object> getParameters() {
        return this.parameters;
    }

    @Override
    public void setSQL(String sql) {
        this.sql = sql;
    }

    @Override
    public String getSQL() {
        return sql;
    }

    @Override
    public List<Object> getArgs() {
        return args;
    }

    @Override
    public void setArgs(Object[] args) {
        if (args != null && args.length > 0) {
            this.args.addAll(Arrays.asList(args));
        }
    }

    @Override
    public Map<String, Object> getProperties() {
        if (properties == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(this.properties);
    }

    @Override
    public void setProperty(String name, Object value) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        this.properties.put(name, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getProperty(String name) {
        return (T) (properties == null ? null : properties.get(name));
    }

    @Override
    public void setSQLCondition(SQLCondition[] sqlAnds) {
        this.sqlCondition = sqlAnds;
    }

    @Override
    public SQLCondition[] getSQLCondition() {
        return this.sqlCondition;
    }

    @Override
    public boolean isAnalysisWithJap() {
        return analysisWithJap;
    }

    @Override
    public void setAnalysisWithJap(boolean analysisWithJap) {
        this.analysisWithJap = analysisWithJap;
    }
}
