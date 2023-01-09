# pom 依赖 以及版本
*  spring boot version 2.6.3 (测试用的版本,具体使用哪个自行测试)
```
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.25</version>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-dbcp2</artifactId>
        </dependency>
        <dependency>
            <groupId>cn.zhangfusheng</groupId>
            <artifactId>paoding-rose-jade</artifactId>
            <version>5.0.7</version>
        </dependency>
        <dependency>
            <groupId>org.apache.shardingsphere</groupId>
            <artifactId>sharding-jdbc-spring-boot-starter</artifactId>
            <version>4.1.1</version>
        </dependency>
```

* application.yml
```yml
spring:
  shardingsphere:
    datasource:
      names: ds0,ds1 # 数据库名称
      ds0: # ds0 库配置
        type: org.apache.commons.dbcp2.BasicDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://127.0.0.1:3306/ds0?characterEncoding=utf-8
        username: root
        password: 123456
      ds1: # ds1
        type: org.apache.commons.dbcp2.BasicDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://127.0.0.1:3306/ds1?characterEncoding=utf-8
        username: root
        password: 123456
    props:
      sql:
        show: true
    sharding:
      tables:
        tb_user:
          actual-data-nodes: ds$->{0..1}.tb_user # 
```