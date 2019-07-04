### SpringBootDuridDemo
### Spring Boot JDBC + Mybatis 配置多数据源 以及 采用Durid 作为连接池




### 1 配置文件
在配置文件中配置两个数据源配置，以及mybatis xml配置文件路径

```poperties
# mybatis  多数据源配置
mybatis.config-location = classpath:mapper/config/mybatis-config.xml

#################  mysql  数据源1 #################
spring.datasource.one.jdbc-url=jdbc:mysql://localhost:3306/user?serverTimezone=UTC&useUnicode=true&characterEncoding=utf-8&useSSL=true
spring.datasource.one.username=root
spring.datasource.one.password=root
#spring.datasource.one.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.one.driver-class-name=com.mysql.jdbc.Driver
#################  mysql  数据源1 #################

#################  mysql  数据源2 ################
spring.datasource.second.jdbc-url=jdbc:mysql://xxxxxxxxxx:3306/user?serverTimezone=UTC&useUnicode=true&characterEncoding=utf-8&useSSL=true
spring.datasource.second.username=root
spring.datasource.second.password=root
#spring.datasource.second.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.second.driver-class-name=com.mysql.jdbc.Driver
#################  mysql  数据源1 #################

```

### 2 数据库配置代码：
###### 1 步骤
1 首先加载配置的数据源：手动将数据配置文件信息注入到数据源实例对象中。
2 根据创建的数据源，配置数据库实例对象注入到SqlSessionFactory 中，构建对应的 SqlSessionFactory。
3 配置数据库事务：将数据源添加到事务中。
4 将SqlSessionFactory 注入到SqlSessionTemplate 模板中
5 最后将上面创建的 SqlSessionTemplate 注入到对应的 Mapper 包路径下，这样这个包下面的 Mapper 都会使用第一个数据源来进行数据库操作。

    basePackages   指明 Mapper 地址。
    sqlSessionTemplateRef    指定 Mapper 路径下注入的 sqlSessionTemplate。


  >在多数据源的情况下，不需要在启动类添加：@MapperScan("com.xxx.mapper") 的注解。


##### 2 项目结构：

![在这里插入图片描述](https://upload-images.jianshu.io/upload_images/7852807-52d34958ad2afb3f.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


![在这里插入图片描述](https://upload-images.jianshu.io/upload_images/7852807-21edca1d17072296.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
##### 3 第一个数据源
```java

@Api("SqlSessionTemplate 注入到对应的 Mapper 包路径下")
@Configuration
@MapperScan(basePackages = "com.example.demo.mapper.one", sqlSessionTemplateRef  = "oneSqlSessionTemplate")
public class OneDataSourceConfig {

    //------------------                 1 加载配置的数据源：   -------------------------------
    @Bean("oneDatasource")
    @ConfigurationProperties(prefix = "spring.datasource.one")
    @Primary //默认是这个库
    public DataSource DataSource1Config(){
        return DataSourceBuilder.create().build();
    }





    //---------------------- 2 创建的数据源 构建对应的 SqlSessionFactory。  ----------------------

    @Bean(name = "oneSqlSessionFactory" )
    @Primary
    public SqlSessionFactory oneSqlSessionFactory(@Qualifier("oneDatasource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:/mapper/one/*.xml"));
        return bean.getObject();
    }


    //------------------------3  配置事务 --------------------------
    @Bean(name = "oneTransactionManager")
    @Primary
    public DataSourceTransactionManager oneTransactionManager(@Qualifier("oneDatasource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }



    //------------------------------- 4 注入 SqlSessionFactory 到 SqlSessionTemplate 中---------------------------------
    @Bean(name = "oneSqlSessionTemplate")
    @Primary
    public SqlSessionTemplate oneSqlSessionTemplate(@Qualifier("oneSqlSessionFactory") SqlSessionFactory sqlSessionFactory) throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

}
```

##### 第二个数据源
```java

@Api("SqlSessionTemplate 注入到对应的 Mapper 包路径下")
@Configuration
@MapperScan(basePackages = "com.example.demo.mapper.second", sqlSessionTemplateRef  = "secondSqlSessionTemplate")
public class SecondDataSourceConfig {


    //------------------                  加载配置的数据源：   -------------------------------


    @Bean("secondDatasource")
    @ConfigurationProperties(prefix = "spring.datasource.second")
    public DataSource DataSource2Config(){
        return DataSourceBuilder.create().build();
    }





    //---------------------- 创建的数据源 构建对应的 SqlSessionFactory。  ----------------------



    @Bean(name = "secondSqlSessionFactory")
    public SqlSessionFactory secondSqlSessionFactory(@Qualifier("secondDatasource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:/mapper/second/*.xml"));
        return bean.getObject();
    }

    //------------------------ 配置事务 --------------------------


    @Bean(name = "secondTransactionManager")
    public DataSourceTransactionManager secondTransactionManager(@Qualifier("secondDatasource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }


    //------------------------------- 注入 SqlSessionFactory 到 SqlSessionTemplate 中---------------------------------


    @Bean(name = "secondSqlSessionTemplate")
    public SqlSessionTemplate secondSqlSessionTemplate(@Qualifier("secondSqlSessionFactory") SqlSessionFactory sqlSessionFactory) throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

}

```


### 3 xml文件
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
</configuration>

```

### 4 mapper 类
```java
public interface User1Mapper {
	public void inserts(User user);
}

```


```java
public interface User2Mapper {
	public void inserts(User user);
}

```

### 5 mybatis  mapper.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">


<mapper namespace="com.example.demo.mapper.one.User1Mapper">
    <insert id="inserts" parameterType="com.example.demo.pojo.User" useGeneratedKeys="true" keyProperty="id">
        insert into user(`name`,age) VALUE (#{name},#{age})
    </insert>
  
</mapper>

```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">


<mapper namespace="com.example.demo.mapper.one.User2Mapper">
    <insert id="inserts" parameterType="com.example.demo.pojo.User" useGeneratedKeys="true" keyProperty="id">
        insert into user(`name`,age) VALUE (#{name},#{age})
    </insert>
  
</mapper>

```

##### 3 启动成功
![在这里插入图片描述](https://upload-images.jianshu.io/upload_images/7852807-76f61f9527df4e0d.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
表示数据源创建成功，这里连接池采用springboot默认的Hikari数据库连接池(不需要配置)

### 6 测试
```java
	@Autowired
	User1Mapper user1Mapper;

	@Autowired
	User2Mapper user2Mapper;
	@Test
	public void test(){
		user1Mapper.inserts(new User(22L, "a123456",1));
		user1Mapper.inserts(new User(33L, "b123456", 1));
		user2Mapper.inserts(new User(44L, "b123456", 1));
	}
```

结果

![在这里插入图片描述](https://upload-images.jianshu.io/upload_images/7852807-84647f486660ba63.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


### 7 更换数据源配置


官方文档 ： https://github.com/alibaba/druid/tree/master/druid-spring-boot-starter

增加配置文件 , 更换为durid数据源
##### 1 配置文件增加配置属性
```

#  StatViewServlet 配置
spring.datasource.druid.stat-view-servlet.login-username=admin
spring.datasource.druid.stat-view-servlet.login-password=admin

# 配置 StatFilter
spring.datasource.druid.filter.stat.log-slow-sql=true
spring.datasource.druid.filter.stat.slow-sql-millis=2000

# Druid 数据源 1 配置
spring.datasource.druid.one.initial-size=3
spring.datasource.druid.one.min-idle=3
spring.datasource.druid.one.max-active=10
spring.datasource.druid.one.max-wait=60000

# Druid 数据源 2 配置
spring.datasource.druid.second.initial-size=6
spring.datasource.druid.second.min-idle=6
spring.datasource.druid.second.max-active=20
spring.datasource.druid.second.max-wait=120000
```

将上面数据库配置文件前缀加上druid

如：


```

#  StatViewServlet 配置
spring.datasource.druid.stat-view-servlet.login-username=admin
spring.datasource.druid.stat-view-servlet.login-password=admin

# 配置 StatFilter
spring.datasource.druid.filter.stat.log-slow-sql=true
spring.datasource.druid.filter.stat.slow-sql-millis=2000

# Druid 数据源 1 配置
spring.datasource.druid.one.initial-size=3
spring.datasource.druid.one.min-idle=3
spring.datasource.druid.one.max-active=10
spring.datasource.druid.one.max-wait=60000

# Druid 数据源 2 配置
spring.datasource.druid.second.initial-size=6
spring.datasource.druid.second.min-idle=6
spring.datasource.druid.second.max-active=20
spring.datasource.druid.second.max-wait=120000

#mybatis.type-aliases-package = com.example.demo.pojo
#################  mysql  数据源1 #################
spring.datasource.druid.one.url=jdbc:mysql://localhost:3306/user?serverTimezone=UTC&useUnicode=true&characterEncoding=utf-8&useSSL=true
spring.datasource.druid.one.username=root
spring.datasource.druid.one.password=root
spring.datasource.druid.one.driver-class-name=com.mysql.jdbc.Driver
#################  mysql  数据源1 #################

#################  mysql  数据源2 #################
spring.datasource.druid.second.url=jdbc:mysql://xxxxxxxxxxx:3306/user?serverTimezone=UTC&useUnicode=true&characterEncoding=utf-8&useSSL=true
spring.datasource.druid.second.username=root
spring.datasource.druid.second.password=root
spring.datasource.druid.second.driver-class-name=com.mysql.jdbc.Driver
#################  mysql  数据源1 #################
```



###### 2 引入依赖

```xml
	<!--druid-->
		<dependency>
			<groupId>com.alibaba</groupId>
			<artifactId>druid-spring-boot-starter</artifactId>
			<version>1.1.10</version>
		</dependency>

```

然后在加载数据源配置哪儿读取配置文件注解改为

    @ConfigurationProperties(prefix = "spring.datasource.druid.one")
    @ConfigurationProperties(prefix = "spring.datasource.druid.second")



##### 启动后发现配置成功
![在这里插入图片描述](https://upload-images.jianshu.io/upload_images/7852807-0e9a1cfdeeab87bc.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)




##### 过程中可能会遇到问题

>Failed to configure a DataSource: 'url' attribute is not specified and no embedded datasource could be configured. 
>Reason: Failed to determine a suitable driver class

原因是：url链接写错了 , SpringBoot duridd无法配置到 数据源url

    错误：spring.datasource.druid.one.jdbc-url=jdbc:mysql://localhost:3306/user?   这是jdbc的url链接
    
    正确 ： spring.datasource.druid.one.url=jdbc:mysql://localhost:3306/user?   这是连接池用的url
    
    
---
源码地址 [传送门][1]


  [1]: https://github.com/pomestyle/SpringBootDuridDemo
