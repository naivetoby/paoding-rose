package net.paoding.rose.jade.annotation.condition;

/**
 * @author fusheng.zhang
 * @Description sql 条件语句封装
 * @create 2020-04-26 13:52:00
 */
public @interface SQLCondition {

    /**
     * and or between and ...  可以跟在 where后的sql 关键字<br/>
     * order by limit ... 非where后的关键字,需要设置 {@link #appendWhere()}为false
     * @return
     */
    String condition() default "and";

    /**
     * 条件表达式,具体请参考 README.md 文档中 2.0 的案例
     * @return
     */
    String value() default "";

    /**
     * 多条件表达式<br/>
     * 条件之间的拼接只能是 condition 的值,具体请参考 README.md 文档中 2.0 的案例
     * @return
     */
    String[] values() default {};

    /**
     * 该组的 value 前是否可以追加 where<br/>
     * 如果Value内已经存在where,该字段值需要赋值为false
     * {@link #condition()} condition 是否为可跟在where条件内的关键字
     * @return
     */
    boolean appendWhere() default true;
}
