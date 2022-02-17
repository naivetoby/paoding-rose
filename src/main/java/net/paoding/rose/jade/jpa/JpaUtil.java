package net.paoding.rose.jade.jpa;

import net.paoding.rose.jade.constant.JadeConstant;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author fusheng.zhang
 * @date 2022-02-17 14:50:40
 */
public class JpaUtil {

    private final String methodName;

    private final Class<?> clazz;

    public JpaUtil(String methodName, Class<?> clazz) {
        this.methodName = methodName;
        this.clazz      = clazz;
    }

    /**
     * 将方法名转换成sql 的where 语句
     * @param fileds    查询字段,多个字段逗号拼接,如果为空则是查询全部字段( *)
     * @param tableName 查询的表名称
     * @return
     */
    public String methodToSql(String fileds, String tableName) {
        String base = "", pre = "", sql = String.format(JadeConstant.SQL_SELECT_FROM_FORMAT, fileds.trim(), tableName.trim());
        StringBuilder current = new StringBuilder();
        PartTree tree = new PartTree(methodName, clazz);
        for (PartTree.OrPart orPart : tree) {
            pre  = current.toString();
            base = Objects.equals(base, "") ? current.toString() : this.or(base.equals(pre) ? pre : base, pre);

            Iterator<Part> parts = orPart.iterator();
            if (!parts.hasNext()) {
                throw new IllegalStateException(String.format("No part found in PartTree %s!", tree));
            }
            current.delete(0, current.length());
            current.append(this.create(parts.next()));
            while (parts.hasNext()) current = this.and(parts.next(), current);
            pre = current.toString();
        }
        base = Objects.equals(base, "") ? pre : this.or(base.equals(pre) ? pre : base, pre);
        if (StringUtils.isNotBlank(base)) {
            sql = String.format(JadeConstant.SQL_WHERE_FORMAT, sql, base.trim());
        }
        Sort sort = tree.getSort();
        if (!sort.equals(Sort.unsorted())) {
            String orderBy = sort.stream()
                    .map(o -> String.format("%s %s", o.getProperty(), o.getDirection()))
                    .collect(Collectors.joining(", "));
            sql = String.format(JadeConstant.SQL_ORDER_BY_FORMAT, sql, orderBy);
        }
        return sql;
    }


    private String create(Part part) {
        return this.form(part);
    }

    private StringBuilder and(Part part, StringBuilder current) {
        return current.append("and ").append(this.form(part));
    }

    private String or(String baseQuery, String query) {
        return baseQuery + "or " + query;
    }

    private String form(Part part) {
        Part.Type type = part.getType();
        String key = part.getProperty().getSegment();
        switch (type) {
            case BETWEEN: return key + " between ? and ? ";
            case IS_NOT_NULL: return key + " is not null ";
            case IS_NULL: return key + " is null ";
            case LESS_THAN: case BEFORE: return key + " < ? ";
            case LESS_THAN_EQUAL: return key + " <= ? ";
            case GREATER_THAN: case AFTER: return key + " > ? ";
            case GREATER_THAN_EQUAL: return key + " >= ? ";
            case NOT_LIKE: return key + " not like ? ";
            case LIKE: return key + " like ? ";
            case STARTING_WITH: return key + " like ?% ";
            case ENDING_WITH: return key + " like %? ";
            case IS_NOT_EMPTY: return key + " is not  ? ";
            // case IS_EMPTY: return key + " like ? ";
            // case NOT_CONTAINING: return key + " like ? ";
            // case CONTAINING: return key + " like ? ";
            case NOT_IN: return key + " not in (?) ";
            case IN: return key + " in (?) ";
            // case NEAR: return key + " like ? ";
            // case WITHIN: return key + " like ? ";
            // case REGEX: return key + " like ? ";
            // case EXISTS: return key + " like ? ";
            case TRUE: return key + " = true ";
            case FALSE: return key + " = false ";
            case NEGATING_SIMPLE_PROPERTY: return key + " <> ? ";
            case SIMPLE_PROPERTY: return key + " = ? ";
            default: throw new RuntimeException("Illegal criteria found '" + type + "'.");
        }
    }
}
