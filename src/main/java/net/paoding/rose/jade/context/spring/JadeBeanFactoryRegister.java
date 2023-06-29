package net.paoding.rose.jade.context.spring;

import net.paoding.rose.jade.annotation.DAO;
import net.paoding.rose.jade.annotation.EnablePaodingRoseJade;
import net.paoding.rose.jade.dataaccess.DataAccessFactory;
import net.paoding.rose.jade.dataaccess.DataAccessFactoryAdapter;
import net.paoding.rose.jade.excetion.JadeException;
import net.paoding.rose.jade.rowmapper.DefaultRowMapperFactory;
import net.paoding.rose.jade.rowmapper.RowMapperFactory;
import net.paoding.rose.jade.scan.RoseJadeCandidateScan;
import net.paoding.rose.jade.statement.InterpreterFactory;
import net.paoding.rose.jade.statement.StatementWrapperProvider;
import net.paoding.rose.jade.statement.cached.CacheProvider;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.context.index.CandidateComponentsIndex;
import org.springframework.context.index.CandidateComponentsIndexLoader;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author fusheng.zhang
 * @date 2021-09-06 13:16:13
 */
public class JadeBeanFactoryRegister implements ImportBeanDefinitionRegistrar, BeanFactoryAware, ResourceLoaderAware, EnvironmentAware {
    private ConfigurableListableBeanFactory beanFactory;
    private Environment environment;
    private ResourceLoader resourceLoader;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (beanFactory instanceof ConfigurableListableBeanFactory) {
            this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 数据存取器工厂，通过它可以得到一个对数据库进行操作的实现
     * <p>
     * @see #getDataAccessFactory()
     */
    private DataAccessFactory dataAccessFactory;

    /**
     * 行映射器工厂，通过它可以得到一个数据库表的行映射器实现，使得从数据库读取的记录可以映射为一个对象
     * <p>
     * @see #getRowMapperFactory()
     */
    private RowMapperFactory rowMapperFactory;

    /**
     * 解释器工厂，通过它可以或得一个DAO方法对应的解析器数组，这些解析器数组将解析每一次DAO操作，进行SQL解析或设置运行时状态
     * <p>
     * @see #getInterpreterFactory()
     */
    private InterpreterFactory interpreterFactory;

    /**
     * StatmentWrapper提供者的bean名称，为“none”等价于null
     */
    private String statmentWrapperProviderName;

    /**
     * 缓存提供者的bean名称，为“none”等价于null
     */
    private String cacheProviderName;


    public DataAccessFactory getDataAccessFactory() {
        if (this.dataAccessFactory == null) {
            dataAccessFactory = new DataAccessFactoryAdapter(new SpringDataSourceFactoryDelegate(beanFactory), beanFactory);
        }
        return dataAccessFactory;
    }

    public InterpreterFactory getInterpreterFactory() {
        if (interpreterFactory == null) {
            interpreterFactory = new SpringInterpreterFactory(beanFactory);
        }
        return interpreterFactory;
    }

    public RowMapperFactory getRowMapperFactory() {
        if (rowMapperFactory == null) {
            rowMapperFactory = new DefaultRowMapperFactory();
        }
        return rowMapperFactory;
    }

    public String getCacheProviderName() {
        if (cacheProviderName == null) {
            String[] names = beanFactory.getBeanNamesForType(CacheProvider.class);
            if (names.length == 0) {
                cacheProviderName = "none";
            } else if (names.length == 1) {
                cacheProviderName = names[0];
            } else {
                String topPriority = "jade.cacheProvider";
                if (ArrayUtils.contains(names, topPriority)) {
                    cacheProviderName = topPriority;
                } else {
                    throw new IllegalStateException(
                            "required not more than 1 CacheProvider, but found " + names.length);
                }
            }
        }
        return "none".equals(cacheProviderName) ? null : cacheProviderName;
    }

    public String getStatementWrapperProvider() {
        if (statmentWrapperProviderName == null) {
            String[] names = beanFactory.getBeanNamesForType(StatementWrapperProvider.class);
            if (names.length == 0) {
                statmentWrapperProviderName = "none";
            } else if (names.length == 1) {
                statmentWrapperProviderName = names[0];
            } else {
                String topPriority = "jade.statmentWrapperProvider";
                if (ArrayUtils.contains(names, topPriority)) {
                    statmentWrapperProviderName = topPriority;
                } else {
                    throw new IllegalStateException(
                            "required not more than 1 StatmentWrapperProvider, but found " + names.length);
                }
            }
        }
        return "none".equals(statmentWrapperProviderName) ? null : statmentWrapperProviderName;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Map<String, Object> annotationAttributes =
                importingClassMetadata.getAnnotationAttributes(EnablePaodingRoseJade.class.getName());
        if (Objects.isNull(annotationAttributes)) return;
        Set<String> scanPackages = new HashSet<>();
        if (annotationAttributes.containsKey("basePackage")) {
            String[] basePackage = (String[]) annotationAttributes.get("basePackage");
            scanPackages.addAll(Arrays.asList(basePackage));
        }
        if (CollectionUtils.isEmpty(scanPackages)) {
            scanPackages.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));
        }

        Set<BeanDefinition> beanDefinitions;

        Set<String> candidateTypes = RoseJadeCandidateScan.doLoadIndex(resourceLoader);
        if (CollectionUtils.isEmpty(candidateTypes)) {
            CandidateComponentsIndex candidateComponentsIndex = CandidateComponentsIndexLoader.loadIndex(this.getClass().getClassLoader());
            if (Objects.nonNull(candidateComponentsIndex)) {
                String stereotype = DAO.class.getName();
                candidateTypes = scanPackages.stream()
                        .map(scanPackage -> candidateComponentsIndex.getCandidateTypes(scanPackage, stereotype))
                        .flatMap(Collection::stream).collect(Collectors.toSet());
            }
        }

        if (!CollectionUtils.isEmpty(candidateTypes)) {
            CachingMetadataReaderFactory cachingMetadataReaderFactory = new CachingMetadataReaderFactory();
            beanDefinitions = candidateTypes.stream().map(type -> {
                try {
                    MetadataReader metadataReader = cachingMetadataReaderFactory.getMetadataReader(type);
                    ScannedGenericBeanDefinition sbd = new ScannedGenericBeanDefinition(metadataReader);
                    sbd.setSource(metadataReader.getResource());
                    return sbd;
                } catch (IOException e) {
                    throw new JadeException(e.getMessage(), e);
                }
            }).collect(Collectors.toSet());
        } else {
            ClassPathScanningCandidateComponentProvider scanner =
                    new ClassPathScanningCandidateComponentProvider(Boolean.FALSE, this.environment) {
                        @Override
                        protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                            return beanDefinition.getMetadata().isIndependent() && !beanDefinition.getMetadata().isAnnotation();
                        }
                    };
            scanner.setResourceLoader(this.resourceLoader);
            scanner.addIncludeFilter(new AnnotationTypeFilter(DAO.class));
            beanDefinitions = scanPackages.stream()
                    .map(scanner::findCandidateComponents).flatMap(Collection::stream).collect(Collectors.toSet());
        }

        beanDefinitions.forEach(beanDefinition -> {
            String daoClassName = Objects.requireNonNull(beanDefinition.getBeanClassName());
            MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
            /*
             * 属性及其设置要按 JadeFactoryBean 的要求来办
             */
            propertyValues.addPropertyValue("objectType", daoClassName);
            propertyValues.addPropertyValue("dataAccessFactory", getDataAccessFactory());
            propertyValues.addPropertyValue("rowMapperFactory", getRowMapperFactory());
            propertyValues.addPropertyValue("interpreterFactory", getInterpreterFactory());
            String cacheProviderName = getCacheProviderName();
            if (cacheProviderName != null) {
                RuntimeBeanReference beanRef = new RuntimeBeanReference(cacheProviderName);
                propertyValues.addPropertyValue("cacheProvider", beanRef);
            }
            String statementWrapperProvider = getStatementWrapperProvider();
            if (statementWrapperProvider != null) {
                RuntimeBeanReference beanRef = new RuntimeBeanReference(statementWrapperProvider);
                propertyValues.addPropertyValue("statementWrapperProvider", beanRef);
            }
            ScannedGenericBeanDefinition scannedBeanDefinition = (ScannedGenericBeanDefinition) beanDefinition;
            scannedBeanDefinition.setPropertyValues(propertyValues);
            scannedBeanDefinition.setBeanClass(JadeFactoryBean.class);
            registry.registerBeanDefinition(daoClassName, scannedBeanDefinition);
        });
    }
}
