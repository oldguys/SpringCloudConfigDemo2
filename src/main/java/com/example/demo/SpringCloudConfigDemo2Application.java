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

	@Bean
	public JdbcEnvironmentRepository jdbcEnvironmentRepository(
			JdbcEnvironmentRepositoryFactory factory,
			JdbcEnvironmentProperties environmentProperties) {
		return factory.build(environmentProperties);
	}

}
