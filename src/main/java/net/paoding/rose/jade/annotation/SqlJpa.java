package net.paoding.rose.jade.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <pre>
 * 用jpa的方式解析sql.
 * 在方法上添加该注解,配置要查询的字段和表名称(优先级最高)
 * 或者在dao层的接口里面添加常量
 *      public final static String SELECT_COLUMN = "id,name....."
 *      public final static String TABLE_NAME = "tb_user"
 * </pre>
 * @author fusheng.zhang
 * @date 2022-02-17 15:26:24
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SqlJpa {

    /**
     * 要查询的字段,多个字段逗号拼接
     * @return
     */
    String columns() default "*";

    /**
     * 要查询的表名称
     * @return
     */
    String tableName();
}
