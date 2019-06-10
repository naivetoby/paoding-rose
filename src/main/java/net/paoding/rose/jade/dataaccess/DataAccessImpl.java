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
package net.paoding.rose.jade.dataaccess;

import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.KeyHolder;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

/**
 * @author 王志亮 [qieqie.wang@gmail.com]
 * @author 廖涵 [in355hz@gmail.com]
 */
public class DataAccessImpl implements DataAccess {

    private final JdbcTemplate jdbcTemplate;

    DataAccessImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ------------------------------------------------

    @Override
    public DataSource getDataSource() {
        return this.jdbcTemplate.getDataSource();
    }

    @Override
    public <T> List<T> select(String sql, Object[] args, RowMapper<T> rowMapper) {
        PreparedStatementCreator csc = getPreparedStatementCreator(sql, args, false);
        return jdbcTemplate.query(csc, new RowMapperResultSetExtractor<T>(rowMapper));
    }

    @Override
    public int update(String sql, Object[] args, KeyHolder generatedKeyHolder) {
        boolean returnKeys = generatedKeyHolder != null;
        PreparedStatementCreator psc = getPreparedStatementCreator(sql, args, returnKeys);
        if (generatedKeyHolder == null) {
            return jdbcTemplate.update(psc);
        } else {
            return jdbcTemplate.update(psc, generatedKeyHolder);
        }
    }

    @Override
    public int[] batchUpdate(String sql, List<Object[]> batchArgs) {
        return jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    private PreparedStatementCreator getPreparedStatementCreator(String sql, Object[] args, boolean returnKeys) {
        return con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            if (returnKeys) {
                ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            }

            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    if (arg instanceof SqlParameterValue) {
                        SqlParameterValue paramValue = (SqlParameterValue) arg;
                        StatementCreatorUtils.setParameterValue(ps, i + 1, paramValue, paramValue.getValue());
                    } else {
                        StatementCreatorUtils.setParameterValue(ps, i + 1, SqlTypeValue.TYPE_UNKNOWN, arg);
                    }
                }
            }
            return ps;
        };
    }

}
