package net.paoding.rose.jade.scan;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author zhangfusheng
 * @date 2023/6/29
 */
public class RoseJadeCandidateScan {

    private static final Logger logger = LoggerFactory.getLogger(RoseJadeCandidateScan.class);

    /**
     * 扫描并获取 META-INF/rose.jade.%s.scanner 的文件内容
     * @param resourceLoader
     * @return
     */
    public static Set<String> doLoadIndex(ResourceLoader resourceLoader) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(resourceLoader);
            Resource[] resources = resolver.getResources("classpath*:META-INF/rose.jade.scanner");
            HashSet<String> daoClass = new HashSet<>();
            for (Resource resource : resources) {
                InputStream inputStream = resource.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line = bufferedReader.readLine();
                ClassLoader classLoader = RoseJadeCandidateScan.class.getClassLoader();
                while (StringUtils.isNotBlank(line)) {
                    try {
                        Class<?> aClass = classLoader.loadClass(line);
                        if (Objects.nonNull(aClass)) daoClass.add(line);
                    } catch (ClassNotFoundException ignored) {
                    } finally {
                        line = bufferedReader.readLine();
                    }
                }
            }
            return daoClass;
        } catch (IOException ignored) {
        }
        return new HashSet<>(0);
    }
}
