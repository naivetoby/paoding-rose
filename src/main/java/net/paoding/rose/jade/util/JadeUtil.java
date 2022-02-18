package net.paoding.rose.jade.util;

/**
 * util
 * @author fusheng.zhang
 * @date 2022-02-18 11:58:18
 */
public class JadeUtil {

    /**
     * 将字符串转换为下划线拼接
     * 例如: createTime 转换成  create_time
     * @param key
     * @return
     */
    public static String getKey(String key) {
        return key.replaceAll("([A-Z])", "_$1").toLowerCase();
    }
}
