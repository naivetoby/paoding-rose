package net.paoding.rose.jade.jpa;

import net.paoding.rose.jade.constant.JadeConstant;
import net.paoding.rose.jade.util.JadeUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

import java.util.Iterator;
import java.util.Objects;

/**
 * @author fusheng.zhang
 * @date 2022-02-17 14:50:40
 */
public class JpaUtil {

    private final String methodName;

    private final Class<?> clazz;

    public JpaUtil(String methodName, Class<?> clazz, Class<?>[] genericReturnTypes) {
        this.methodName = methodName;
        if (Objects.nonNull(genericReturnTypes) && genericReturnTypes.length == 1) {
            this.clazz = genericReturnTypes[0];
        } else {
            this.clazz = clazz;
        }
    }

    /**
     * 将方法名转换成sql 的where 语句
     * @param fileds    查询字段,多个字段逗号拼接,如果为空则是查询全部字段( *)
     * @param tableName 查询的表名称
     * @return
     */
    public JpaDetail methodToSql(String fileds, String tableName) {
        String base = "", pre = "",
                sql = String.format(JadeConstant.SQL_SELECT_FROM_FORMAT, fileds.trim(), tableName.trim());
        StringBuilder current = new StringBuilder();
        PartTree tree = new PartTree(methodName, clazz);
        for (PartTree.OrPart orPart : tree) {
            pre = current.toString();
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
        return new JpaDetail(sql, tree.getSort());
    }


    private String create(Part part) {
        return this.form(part);
    }

    private StringBuilder and(Part part, StringBuilder current) {
        return current.append(JadeConstant.SQL_AND).append(this.form(part));
    }

    private String or(String baseQuery, String query) {
        return baseQuery + JadeConstant.SQL_OR + query;
    }

    private String form(Part part) {
        Part.Type type = part.getType();
        String key = part.getProperty().getSegment();
        switch (type) {
            case BETWEEN: return JadeUtil.getKey(key) + " between ? and ? ";
            case IS_NOT_NULL: return JadeUtil.getKey(key) + " is not null ";
            case IS_NULL: return JadeUtil.getKey(key) + " is null ";
            case LESS_THAN: case BEFORE: return JadeUtil.getKey(key) + " < ? ";
            case LESS_THAN_EQUAL: return JadeUtil.getKey(key) + " <= ? ";
            case GREATER_THAN: case AFTER: return JadeUtil.getKey(key) + " > ? ";
            case GREATER_THAN_EQUAL: return JadeUtil.getKey(key) + " >= ? ";
            case NOT_LIKE: return JadeUtil.getKey(key) + " not like ? ";
            case LIKE: return JadeUtil.getKey(key) + " like ? ";
            case STARTING_WITH: return JadeUtil.getKey(key) + " like ?% ";
            case ENDING_WITH: return JadeUtil.getKey(key) + " like %? ";
            case IS_NOT_EMPTY: return JadeUtil.getKey(key) + " is not  ? ";
            // case IS_EMPTY: return JadeUtil.getKey(key) + " like ? ";
            // case NOT_CONTAINING: return JadeUtil.getKey(key) + " like ? ";
            // case CONTAINING: return JadeUtil.getKey(key) + " like ? ";
            case NOT_IN: return JadeUtil.getKey(key) + " not in (?) ";
            case IN: return JadeUtil.getKey(key) + " in (?) ";
            // case NEAR: return JadeUtil.getKey(key) + " like ? ";
            // case WITHIN: return JadeUtil.getKey(key) + " like ? ";
            // case REGEX: return JadeUtil.getKey(key) + " like ? ";
            // case EXISTS: return JadeUtil.getKey(key) + " like ? ";
            case TRUE: return JadeUtil.getKey(key) + " = true ";
            case FALSE: return JadeUtil.getKey(key) + " = false ";
            case NEGATING_SIMPLE_PROPERTY: return JadeUtil.getKey(key) + " <> ? ";
            case SIMPLE_PROPERTY: return JadeUtil.getKey(key) + " = ? ";
            default: throw new RuntimeException("Illegal criteria found '" + type + "'.");
        }
    }


}
