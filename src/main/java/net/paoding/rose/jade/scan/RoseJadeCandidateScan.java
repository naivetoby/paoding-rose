package net.paoding.rose.jade.scan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
            Resource[] resources = resolver.getResources("classpath*:META-INF/rose.jade.scanner/**");
            return Arrays.stream(resources).map(Resource::getFilename).collect(Collectors.toSet());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return new HashSet<>(0);
    }
}
