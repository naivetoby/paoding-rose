# 多数据源文档

> 以下实现均基于spring boot

```xml

<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-dbcp2</artifactId>
    <version>2.9.0</version>
</dependency>
<dependency>
<groupId>mysql</groupId>
<artifactId>mysql-connector-java</artifactId>
<version>8.0.25</version>
</dependency>
<dependency>
<groupId>cn.zhangfusheng</groupId>
<artifactId>paoding-rose-jade</artifactId>
<version>5.0.5</version>
</dependency>
```

## 多数据源

```java
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

public class DatasourceConfig {
    @Bean("jade.dataSourceFactory.db1")
    public DataSourceFactory db1() {
        BasicDataSource dataSource = DataSourceBuilder.create(this.getClass().getClassLoader())
                .type(BasicDataSource.class)
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .url("jdbc:mysql://192.168.18.191:3306/open-server?useSSL=true&serverTimezone=GMT%2B8&useUnicode=true&autoReconnect=true&characterEncoding=UTF-8")
                .username("root")
                .password("1234").build();
        return new SimpleDataSourceFactory(dataSource);
    }

    @Bean("jade.dataSourceFactory.db2")
    public DataSourceFactory db2() {
        BasicDataSource dataSource = DataSourceBuilder.create(this.getClass().getClassLoader())
                .type(BasicDataSource.class)
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .url("jdbc:mysql://192.168.18.191:3306/user-server?useSSL=true&serverTimezone=GMT%2B8&useUnicode=true&autoReconnect=true&characterEncoding=UTF-8")
                .username("root")
                .password("1234").build();
        return new SimpleDataSourceFactory(dataSource);
    }
}
```

```java
@DAO(catalog = "db1")
public interface TbAddressDao {
    // 会查询db1 对应的open-server
    @SQL("select * from tb_address limit 1")
    Map<String, Object> findOne();
}
```

```java
@DAO(catalog = "db2")
public interface TbUserDao {
    // 会查询db2 对应的 user-server
    @SQL("select * from tb_user limit 1")
    Map<Object, Object> findOne();
}
```

## 读写分离