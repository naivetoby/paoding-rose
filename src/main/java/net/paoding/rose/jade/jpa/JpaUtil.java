package net.paoding.rose.jade.jpa;

import net.paoding.rose.jade.annotation.SQLType;
import net.paoding.rose.jade.annotation.SqlJpa;
import net.paoding.rose.jade.constant.JadeConstant;
import net.paoding.rose.jade.excetion.JadeException;
import net.paoding.rose.jade.statement.DAOMetaData;
import net.paoding.rose.jade.statement.StatementMetaData;
import net.paoding.rose.jade.util.JadeUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * @author fusheng.zhang
 * @date 2022-02-17 14:50:40
 */
public class JpaUtil {
    private static final Log sqlLogger = LogFactory.getLog(StatementMetaData.class);

    private final SqlJpa sqlJpa;

    private final PartTree tree;
    /**
     * 查询字段,多个字段逗号拼接,如果为空则是查询全部字段( *)
     */
    private final String fields;
    /**
     * 查询的表名称
     */
    private final String tableName;

    public JpaUtil(String fields, String tableName, String methodName, Class<?> domainClass, SqlJpa sqlJpa) {
        this.sqlJpa = sqlJpa;
        this.fields = fields;
        this.tableName = tableName;
        this.tree = new PartTree(methodName, domainClass);
    }

    public JpaUtil(DAOMetaData daoMetaData, Method method, Class<?> domainClass, Class<?>[] genericReturnTypes) {

        this.sqlJpa = method.getAnnotation(SqlJpa.class);
        boolean nonNull = Objects.nonNull(sqlJpa);
        if (nonNull && !Objects.equals(sqlJpa.domainClass(), Void.class)) {
            domainClass = sqlJpa.domainClass();
        } else if (Objects.nonNull(genericReturnTypes) && genericReturnTypes.length == 1) {
            domainClass = genericReturnTypes[0];
        }

        this.tree = new PartTree(method.getName(), domainClass);
        // 要查询的字段
        String columnFields = nonNull && !StringUtils.equals(sqlJpa.columns(), "*")
                ? sqlJpa.columns() : daoMetaData.getConstant(JadeConstant.SELECT_COLUMN);
        if (StringUtils.isBlank(columnFields)) {
            columnFields = "*";
            sqlLogger.warn(String.format("请在方法[%s]添加SqlJpa注解,或者在[%s]添加常量SELECT_COLUMN常量,指定要查询的字段",
                    method.getName(), daoMetaData.getDAOClass()));
        }
        // 要查询的表
        String tableName = nonNull && StringUtils.isNotBlank(sqlJpa.tableName())
                ? sqlJpa.tableName() : daoMetaData.getConstant(JadeConstant.TABLE_NAME);
        if (StringUtils.isBlank(tableName)) {
            throw new JadeException("请在方法[{}]添加SqlJpa注解,或者在[{}]添加常量TABLE_NAME常量,指定要查询表",
                    method.getName(), daoMetaData.getDAOClass());
        }
        this.fields = columnFields;
        this.tableName = tableName;
    }

    /**
     * 将方法名转换成sql 的where 语句
     * @return
     */
    public JpaDetail methodToSql(Object[] args) {
        String sql = String.format(JadeConstant.SQL_SELECT_FROM_FORMAT, fields.trim(), tableName.trim());

        if (this.tree.isCountProjection()) {
            if (Objects.isNull(this.sqlJpa) || Objects.equals(this.sqlJpa.domainClass(), Void.class)) {
                throw new JadeException("count 语句必须配置 SqlJpa.domain ");
            }
            sql = String.format(JadeConstant.SQL_COUNT_FROM_FORMAT, tableName.trim());
        } else if (this.tree.isDelete()) {
            if (Objects.isNull(this.sqlJpa) || Objects.equals(this.sqlJpa.domainClass(), Void.class)) {
                throw new JadeException("delete 语句必须配置 SqlJpa.domain ");
            }
            sql = String.format(JadeConstant.SQL_DELETE_FROM_FORMAT, tableName.trim());
        } else if (this.tree.isDistinct() || this.tree.isExistsProjection()) {
            throw new JadeException("暂不支持 distinct 和 exists sql查询");
        }


        String base = "", pre = "";
        List<Object> args_ = new ArrayList<>(args.length);
        int nexArgSize;
        Iterator<Object> argsIterator = Arrays.stream(args).iterator();
        StringBuilder current = new StringBuilder();

        for (PartTree.OrPart orPart : tree) {
            pre = current.toString();
            base = Objects.equals(base, "") ? current.toString() : this.or(base.equals(pre) ? pre : base, pre);

            Iterator<Part> parts = orPart.iterator();
            if (!parts.hasNext()) {
                throw new IllegalStateException(String.format("No part found in PartTree %s!", tree));
            }
            current.delete(0, current.length());
            nexArgSize = getNexArgSizeAndAddArg(args_, argsIterator);
            current.append(this.create(parts.next(), nexArgSize));
            while (parts.hasNext()) {
                nexArgSize = getNexArgSizeAndAddArg(args_, argsIterator);
                current = this.and(parts.next(), current, nexArgSize);
            }
            pre = current.toString();
        }
        base = Objects.equals(base, "") ? pre : this.or(base.equals(pre) ? pre : base, pre);
        if (StringUtils.isNotBlank(base)) {
            sql = String.format(JadeConstant.SQL_WHERE_FORMAT, sql, base.trim());
        }
        return new JpaDetail(sql, tree.getSort(), args_, tree.getMaxResults());
    }

    @SuppressWarnings("rawtypes")
    private int getNexArgSizeAndAddArg(List<Object> args, Iterator<Object> argsIterator) {
        int nexArgSize = 0;
        Object nextArg = argsIterator.next();
        if (nextArg instanceof Collection) {
            Collection nextArg_ = (Collection) nextArg;
            nexArgSize = (nextArg_).size();
            args.addAll(nextArg_);
        } else {
            args.add(nextArg);
        }
        return nexArgSize;
    }

    private String create(Part part, int paramLength) {
        return this.form(part, paramLength);
    }

    private StringBuilder and(Part part, StringBuilder current, int paramLength) {
        return current.append(JadeConstant.SQL_AND).append(this.form(part, paramLength));
    }

    private String or(String baseQuery, String query) {
        return baseQuery + JadeConstant.SQL_OR + query;
    }

    private String form(Part part, int paramLength) {
        Part.Type type = part.getType();
        String key = part.getProperty().getSegment();
        switch (type) {
            case BETWEEN:
                return JadeUtil.getKey(key) + " between ? and ? ";
            case IS_NOT_NULL:
                return JadeUtil.getKey(key) + " is not null ";
            case IS_NULL:
                return JadeUtil.getKey(key) + " is null ";
            case LESS_THAN:
            case BEFORE:
                return JadeUtil.getKey(key) + " < ? ";
            case LESS_THAN_EQUAL:
                return JadeUtil.getKey(key) + " <= ? ";
            case GREATER_THAN:
            case AFTER:
                return JadeUtil.getKey(key) + " > ? ";
            case GREATER_THAN_EQUAL:
                return JadeUtil.getKey(key) + " >= ? ";
            case NOT_LIKE:
                return JadeUtil.getKey(key) + " not like ? ";
            case LIKE:
                return JadeUtil.getKey(key) + " like ? ";
            case STARTING_WITH:
                return JadeUtil.getKey(key) + " like ?% ";
            case ENDING_WITH:
                return JadeUtil.getKey(key) + " like %? ";
            case IS_NOT_EMPTY:
                return JadeUtil.getKey(key) + " is not  ? ";
            // case IS_EMPTY: return JadeUtil.getKey(key) + " like ? ";
            // case NOT_CONTAINING: return JadeUtil.getKey(key) + " like ? ";
            // case CONTAINING: return JadeUtil.getKey(key) + " like ? ";
            case NOT_IN:
                return JadeUtil.getKey(key) + " not in ( " + getQuestionMark(paramLength) + " ) ";
            case IN:
                return JadeUtil.getKey(key) + " in ( " + getQuestionMark(paramLength) + " ) ";
            // case NEAR: return JadeUtil.getKey(key) + " like ? ";
            // case WITHIN: return JadeUtil.getKey(key) + " like ? ";
            // case REGEX: return JadeUtil.getKey(key) + " like ? ";
            // case EXISTS: return JadeUtil.getKey(key) + " like ? ";
            case TRUE:
                return JadeUtil.getKey(key) + " = true ";
            case FALSE:
                return JadeUtil.getKey(key) + " = false ";
            case NEGATING_SIMPLE_PROPERTY:
                return JadeUtil.getKey(key) + " <> ? ";
            case SIMPLE_PROPERTY:
                return JadeUtil.getKey(key) + " = ? ";
            default:
                throw new RuntimeException("Illegal criteria found '" + type + "'.");
        }
    }

    private String getQuestionMark(int num) {
        String[] strings = new String[num];
        Arrays.fill(strings, "?");
        return String.join(" ,", strings);
    }

    public SQLType sqlType() {
        return this.tree.isDelete() ? SQLType.WRITE : SQLType.READ;
    }

}
