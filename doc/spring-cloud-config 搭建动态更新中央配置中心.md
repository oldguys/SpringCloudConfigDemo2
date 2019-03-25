##spring-cloud-config 搭建动态更新中央配置中心

> spring-cloud-config 模块提供了统一配置服务中心，可以进行将所有应用的配置文件存放一处，便于维护与更新。在这个基础上，可以实现一个思路：“在配置中心端使用jdbc才操作配置数据，当更新数据库中配置时，通知接入的需要更新的App，使得App热刷新配置信息。”
>
> 相关资料:
>1.  spring-cloud-bus 官方文档:
https://cloud.spring.io/spring-cloud-static/spring-cloud-bus/2.1.0.RELEASE/multi/multi_spring-cloud-bus.html
>2. spring-cloud-config 官方文档: 
https://cloud.spring.io/spring-cloud-static/spring-cloud-config/2.1.0.RELEASE/multi/multi_spring-cloud-config.html) 
>
>3. 使用Eurake 的config-server ：https://github.com/oldguys/SpringCloudConfigDemo2
> 4. 使用Eurake 的config-client https://github.com/oldguys/SpringCloudServerEurekaDemo1
>5.  不使用Eurake 的config-client
https://github.com/oldguys/SpringCloudClientDemo1
>


#### 具体实现 
step 1. 引入Maven
基于jdbc配置中心的基本引入方式
```
<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-config-server</artifactId>
</dependency>

<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
<dependency>
	<groupId>mysql</groupId>
	<artifactId>mysql-connector-java</artifactId>
	<version>5.1.47</version>
</dependency>
<dependency>
	<groupId>com.alibaba</groupId>
	<artifactId>druid</artifactId>
	<version>1.0.29</version>
</dependency>
```
######（增强，非必须） 添加Eurake高可用
```
<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```
step 2. 配置配置文件
###### 配置服务器中心端
```
spring:
  datasource:
    username: root
    password: root
    url: jdbc:mysql://127.0.0.1:3306/spring-cloud-config-demo?useUnicode=true&characterEncoding=utf8&useSSL=false
    driver-class-name: com.mysql.jdbc.Driver
    type: com.alibaba.druid.pool.DruidDataSource
  application:
    name: config-server
  cloud:
    config:
      server:
#        git:
#          uri: https://github.com/forezp/SpringcloudConfig/
#          search-paths: respo
#          username:
#          password:
#          order: 1
        jdbc:
          order: 0
          sql: SELECT _KEY, _VALUE from PROPERTIES where APPLICATION=? and PROFILE=? and LABEL=?
      label: master

```
######注意：
其中SQL属性如果是使用默认【"SELECT KEY, VALUE from PROPERTIES  where APPLICATION=? and PROFILE=? and LABEL=? 】，“KEY”，“VALUE ”会导致关键字冲突的异常。

```
package com.example.demo;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.config.server.environment.JdbcEnvironmentProperties;
import org.springframework.cloud.config.server.environment.JdbcEnvironmentRepository;
import org.springframework.cloud.config.server.environment.JdbcEnvironmentRepositoryFactory;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.activation.DataSource;

/**
 *
 *
 *
 源码：org.springframework.cloud.config.server.config.JdbcRepositoryConfiguration

 @Configuration
 @Profile("jdbc")
 @ConditionalOnClass(JdbcTemplate.class)
 class JdbcRepositoryConfiguration {

 @Bean
 @ConditionalOnBean(JdbcTemplate.class)
 public JdbcEnvironmentRepository jdbcEnvironmentRepository(
 JdbcEnvironmentRepositoryFactory factory,
 JdbcEnvironmentProperties environmentProperties) {
 return factory.build(environmentProperties);
 }

 }
 *
 */
@EnableDiscoveryClient
@EnableEurekaClient
@EnableConfigServer
@SpringBootApplication
public class SpringCloudConfigDemo2Application {

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudConfigDemo2Application.class, args);
	}

    @ConfigurationProperties(prefix = "spring.datasource")
	@Bean
	public DruidDataSource dataSource(){
		return new DruidDataSource();
	}

	@Bean
	public JdbcTemplate jdbcTemplate(DruidDataSource dataSource){
		return new JdbcTemplate(dataSource);
	}

	/**
	 *  手动注入
	 * @param factory
	 * @param environmentProperties
	 * @return
	 */
	@Bean
	public JdbcEnvironmentRepository jdbcEnvironmentRepository(
			JdbcEnvironmentRepositoryFactory factory,
			JdbcEnvironmentProperties environmentProperties) {
		return factory.build(environmentProperties);
	}

}

```
######注意：
1. 在默认配置中，开启不了jdbc配置，可以通过自己注入
2. 使用注解 @EnableConfigServer 开启配置中心服务
3. （增强，不必选）使用注解 @EnableDiscoveryClient
@EnableEurekaClient 进行接入Eureka ，“@EnableDiscoveryClient”使用配置中心时必须使用此注解，在扫描阶段会根据这个来进行扫描，如果没有则无法开启通过appName来作为路径。

step 3：配置客户端

###### 1. 引入Maven
```
<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-starter-config</artifactId>
</dependency>

<!-- 使用消息主线进行通知,spring-cloud 默认兼容rabbitMq和kafka -->
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-starter-bus-amqp</artifactId>
</dependency>
```
（增强：使用Eurake进行负载）
```
<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

###### 2. 核心配置文件“application.yml”
```
spring:
  application:
    name: config-client
  cloud:
    config:
      label: master
      profile: dev
#      uri: http://localhost:8086/
#      enabled: true
    bus:
      enabled: true
      trace:
        enabled: true
management:
  endpoints:
    web:
      exposure:
        include: bus-refresh
server:
  port: 8087
```
注意：
```
management.endpoints.web.exposure.include: bus-refresh
```
开启通过http接口触发拉配置中心数据，接口【http://localhost:8087/actuator/bus-refresh】
其中 management.endpoints.web.exposure.include: **bus-refresh** 和 management.endpoints.web.exposure.include: **refresh** 效果不相同,
**bus-refresh**：加入消息主线spring-cloud-bus，调用接口不触发返回值；
**refresh** ：不加入消息主线，调用接口返回 被更新的配置数据标识数组。


###### 3. 属性入口配置文件 bootstrap.yml
```
spring:
  cloud:
    config:
      uri: http://localhost:8080/
      label: master
      profile: dev
```
注意：
1. 如果该配置直接配在 application.yml 文件中，可能导致扫描不到该属性，必须新建bootstrap.yml 文件才有效果
 2. uri默认是 【http://localhost:8888/】

如果使用Eurake配置中心的，可以写成：
```
spring:
  cloud:
    config:
#      uri: http://localhost:8080/
      label: master
      profile: dev
#      name: config-client
      discovery:
        service-id: CONFIG-SERVER
        enabled: true
```
注意：
1. spring.cloud.config.name = config-client 为读取环境变量的前缀，如果不写，则默认使用spring.appliaction.name作为环境变量名前缀
2. spring.cloud.config.discovery.service-id= 配置中心的 spring.appliaction.name。另外，必须 spring.cloud.config.discovery.enabled = true ，才能开启使用服务名作为索引。

###### 4. 客户端入口
```
package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@RefreshScope
@EnableEurekaClient
@EnableDiscoveryClient
@SpringBootApplication
public class SpringCloudClientEurekaDemo1Application {

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudClientEurekaDemo1Application.class, args);
	}
}

```
注意：
1. @RefreshScope ： 启用使用接口刷新配置中心数据，接口[http://localhost:8087/actuator/bus-refresh]
2. @EnableEurekaClient & @EnableDiscoveryClient 同上


###### 5. 监听客户端更新事件
**RefreshRemoteApplicationEvent** 事件是spring-cloud-config提供的配置属性更新事件，所以通过监听，来进行后续容器更新处理。
```
package com.example.demo.listeners;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.bus.event.RefreshRemoteApplicationEvent;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;


/**
 * Created by Administrator on 2019/3/22 0022.
 */
@Component
public class TestRefreshListener
        implements ApplicationListener<RefreshRemoteApplicationEvent> {

    private ContextRefresher contextRefresher;

    @Override
    public void onApplicationEvent(RefreshRemoteApplicationEvent event) {
        System.out.println();
        System.out.println("TestRefreshListener。。。。。。。。。。。。");
        System.out.println();
    }
}
```
注意：
1. management.endpoints.web.exposure.include = **bus-refresh** 的时候，才能触发更新，如果 **refresh** 则不会触发该事件。