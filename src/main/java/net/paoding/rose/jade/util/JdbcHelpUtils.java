package net.paoding.rose.jade.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @author fusheng.zhang
 * @date 2020-10-13 11:04:49
 */
public class JdbcHelpUtils {

    /**
     * jdk 8 时间类型处理
     * @param rs
     * @param index
     * @param requiredType
     * @param oldValue
     * @return
     * @throws SQLException
     */
    public static Object getTimeValue(ResultSet rs, int index, Class<?> requiredType, Object oldValue) throws SQLException {
        if (requiredType.equals(LocalDate.class)) {
            return rs.getTimestamp(index).toLocalDateTime().toLocalDate();
        } else if (requiredType.equals(LocalTime.class)) {
            return rs.getTimestamp(index).toLocalDateTime().toLocalTime();
        } else if (requiredType.equals(LocalDateTime.class)) {
            return rs.getTimestamp(index).toLocalDateTime();
        }
        return oldValue;
    }
}
