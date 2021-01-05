package net.paoding.rose.jade.context.spring;

import net.paoding.rose.jade.annotation.DAO;
import net.paoding.rose.jade.dataaccess.DataSourceFactory;
import net.paoding.rose.jade.dataaccess.DataSourceHolder;
import net.paoding.rose.jade.statement.StatementMetaData;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author qieqie
 */
public class SpringDataSourceFactoryDelegate implements DataSourceFactory {

    private final ListableBeanFactory beanFactory;

    private final Map<String, DataSourceFactory> dataSourceFactoryMap = new HashMap<>();

    public SpringDataSourceFactoryDelegate(ListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public DataSourceHolder getHolder(StatementMetaData metaData, Map<String, Object> runtimeProperties) {
        Class<?> daoClass = metaData.getDAOMetaData().getDAOClass();
        String catalog = daoClass.getAnnotation(DAO.class).catalog();
        DataSourceFactory dataSourceFactory = null;
        String key = daoClass.getName() + "-" + metaData.getSQLType();
        if (dataSourceFactoryMap.containsKey(key)) {
            dataSourceFactory = dataSourceFactoryMap.get(key);
        } else {
            String beanName = "jade.dataSourceFactory";
            String catlogBeanName = StringUtils.isEmpty(catalog) ? beanName : beanName + "." + catalog;
            ListableBeanFactory beanFactory = this.beanFactory;
            if (beanFactory != null) {
                if (beanFactory.containsBeanDefinition(catlogBeanName) || beanFactory.containsBean(catlogBeanName)) {
                    dataSourceFactory = beanFactory.getBean(catlogBeanName, DataSourceFactory.class);
                } else if (beanFactory.containsBeanDefinition(beanName) || beanFactory.containsBean(beanName)) {
                    dataSourceFactory = beanFactory.getBean(beanName, DataSourceFactory.class);
                } else {
                    dataSourceFactory = new SpringDataSourceFactory(beanFactory);
                }
                dataSourceFactoryMap.put(key, dataSourceFactory);
            }
        }
        assert dataSourceFactory != null;
        return dataSourceFactory.getHolder(metaData, runtimeProperties);
    }

}
