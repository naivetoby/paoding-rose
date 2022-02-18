package net.paoding.rose.jade.jpa;

import net.paoding.rose.jade.constant.JadeConstant;
import net.paoding.rose.jade.util.JadeUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author fusheng.zhang
 * @date 2022-02-17 20:34:22
 */
public class JpaDetail {

    private final String sql;

    private final List<String> orderBy;

    public JpaDetail(String sql, Sort sort) {
        this.sql = sql;
        if (!sort.equals(Sort.unsorted())) {
            this.orderBy = sort.stream()
                    .map(o -> String.format("%s %s", JadeUtil.getKey(o.getProperty()), o.getDirection()))
                    .collect(Collectors.toList());
        } else {
            this.orderBy = new ArrayList<>(0);
        }
    }

    public String getSql() {
        return sql;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public String sql(Optional<Pageable> optional) {
        List<String> nextOrderBy = new ArrayList<>(0);
        String sql = this.sql, limit = "";
        if (optional.isPresent()) {
            Pageable pageable = optional.get();
            Sort sort = pageable.getSort();
            if (!sort.equals(Sort.unsorted())) {
                nextOrderBy = sort.stream()
                        .map(o -> String.format("%s %s", JadeUtil.getKey(o.getProperty()), o.getDirection()))
                        .collect(Collectors.toList());
            }
            limit = String.format("%s,%s", pageable.getOffset(), pageable.getPageSize());
        }
        nextOrderBy.addAll(orderBy);
        String orderBy = StringUtils.join(nextOrderBy, ", ");
        if (StringUtils.isNotBlank(orderBy)) sql = String.format(JadeConstant.SQL_ORDER_BY_FORMAT, sql, orderBy);
        if (StringUtils.isNotBlank(limit)) sql = String.format(JadeConstant.SQL_LIMIT_FORMAT, sql, limit);
        return sql;
    }

}
