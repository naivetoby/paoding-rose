package net.paoding.rose.jade.annotation.condition;

/**
 * @author fusheng.zhang
 * @Description sql 条件语句封装
 * @create 2020-04-26 13:52:00
 */
public @interface SQLCondition {

    /**
     * and 或者 or
     * 注: 目前只测试了 and 和 or的使用
     * 注: 支持 order by 和 limit ,当值为order by  或者 limit 时,此处需要赋值为空穿,具体的分页和排序 需要写在 value中
     * @return
     */
    String condition() default "and";

    /**
     * 条件表达式,具体请参考 README.md 文档中 2.0 的案例
     * @return
     */
    String value() default "";

    /**
     * 多条件表达式
     * 条件之间的拼接只能是 condition 的值,具体请参考 README.md 文档中 2.0 的案例
     * @return
     */
    String[] values() default {};
}
