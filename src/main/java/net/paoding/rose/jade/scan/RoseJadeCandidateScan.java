package net.paoding.rose.jade.scan;

import org.apache.commons.lang.StringUtils;
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
            if (resources.length == 0) return new HashSet<>(0);
            Resource resource = resources[0];
            InputStream inputStream = resource.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            HashSet<String> daoClass = new HashSet<>();
            String line = bufferedReader.readLine();
            while (StringUtils.isNotBlank(line)) {
                daoClass.add(line);
                line = bufferedReader.readLine();
            }
            return daoClass;
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return new HashSet<>(0);
    }
}
