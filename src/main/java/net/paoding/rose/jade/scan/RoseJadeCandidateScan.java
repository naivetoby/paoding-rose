package net.paoding.rose.jade.scan;

import org.apache.commons.collections.CollectionUtils;
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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zhangfusheng
 * @date 2023/6/29
 */
public class RoseJadeCandidateScan {

    private static final Logger logger = LoggerFactory.getLogger(RoseJadeCandidateScan.class);

    public static Set<String> doLoadIndex(ResourceLoader resourceLoader) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(resourceLoader);
            Resource[] resources = resolver.getResources("classpath*:META-INF/rose.jade.*.scanner");

            return Arrays.stream(resources).map(o -> {
                try (InputStream inputStream = o.getInputStream();
                     InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.defaultCharset());
                     BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                    ArrayList<String> daoClass = new ArrayList<>();
                    String line = bufferedReader.readLine();
                    while (StringUtils.isNotBlank(line)) {
                        daoClass.add(line);
                        line = bufferedReader.readLine();
                    }
                    return daoClass;
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
                return new ArrayList<String>(0);
            }).filter(CollectionUtils::isNotEmpty).flatMap(Collection::stream).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return new HashSet<>(0);
    }
}
