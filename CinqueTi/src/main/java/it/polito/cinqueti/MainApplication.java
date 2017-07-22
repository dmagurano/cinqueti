package it.polito.cinqueti;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.apache.catalina.connector.Connector;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;



@SpringBootApplication
@EntityScan(basePackages = "it.polito.cinqueti.entities")
@EnableJpaRepositories(basePackages = "it.polito.cinqueti.repositories")


public class MainApplication {
	
	@Value("${spring.datasource.postgres.url}")
	private String postgresUrl;
	
	@Value("${spring.datasource.postgres.username}")
	private String postgresUsername;
	
	@Value("${spring.datasource.postgres.password}")
	private String postgresPsw;
	
	
	@Bean
	public EmbeddedServletContainerFactory  servletContainer() {
		TomcatEmbeddedServletContainerFactory  tomcat = new TomcatEmbeddedServletContainerFactory();
		tomcat.addAdditionalTomcatConnectors(createStandardConnector());
		return tomcat;
	}
	
	@Bean(destroyMethod = "close")
    public DataSource dataSource() {
        
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl(postgresUrl);
        ds.setUsername(postgresUsername);
        ds.setPassword(postgresPsw);
        
        return ds;
        
    }
	
	
	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource){
		
		HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		vendorAdapter.setDatabase(Database.POSTGRESQL);
		vendorAdapter.setGenerateDdl(true);
		
		LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
		
		factory.setJpaVendorAdapter(vendorAdapter);
		factory.setPackagesToScan("it.polito.cinqueti.entities");
		factory.setDataSource(dataSource);
		return factory;
		
	}
	
	@Bean
	public PlatformTransactionManager transactionManager(EntityManagerFactory factory){
		
		JpaTransactionManager txManager = new JpaTransactionManager();
		txManager.setEntityManagerFactory(factory);
		return txManager;
	}

	private Connector createStandardConnector() {
		Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
		connector.setPort(8080);
		return connector;
	}

	public static void main(String[] args) {
		SpringApplication.run(MainApplication.class, args);
	}
}
