package net.paoding.rose.jade.constant;

/**
 * @author fusheng.zhang
 * @date 2022-02-17 15:19:36
 */
public class JadeConstant {

    public final static String SELECT_COLUMN = "SELECT_COLUMN";
    public final static String TABLE_NAME = "TABLE_NAME";

    public final static String SQL_SELECT = "SELECT";
    public final static String SQL_FROM = "FROM";
    public final static String SQL_WHERE = "WHERE";
    public final static String SQL_ORDER_BY = "ORDER BY";

    public final static String SQL_SELECT_FROM_FORMAT = SQL_SELECT + " %s " + SQL_FROM + " %s";
    public final static String SQL_WHERE_FORMAT = "%s " + SQL_WHERE + " %s";
    public final static String SQL_ORDER_BY_FORMAT = "%s " + SQL_ORDER_BY + " %s";
}
