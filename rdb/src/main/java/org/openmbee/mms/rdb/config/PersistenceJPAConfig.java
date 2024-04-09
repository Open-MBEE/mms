package org.openmbee.mms.rdb.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Properties;
import javax.sql.DataSource;

import org.openmbee.mms.core.config.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(
    basePackages = {"org.openmbee.mms.data.domains.global", "org.openmbee.mms.rdb.repositories"},
    entityManagerFactoryRef = "defaultEntityManager",
    transactionManagerRef = "defaultTransactionManager"
)
@EnableTransactionManagement
public class PersistenceJPAConfig {

    private Environment env;

    @Autowired
    public void setEnv(Environment env) {
        this.env = env;
    }

    @Primary
    @Bean(name = "defaultEntityManager")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(getDatasource());
        em.setPackagesToScan(
            "org.openmbee.mms.data.domains.global",
            "org.openmbee.mms.rdb.repositories"
        );

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaProperties(additionalProperties());
        em.setPersistenceUnitName("defaultEntityManager");

        return em;
    }

    @Primary
    @Bean
    public DataSource getDatasource() {
        if (env.containsProperty("spring.datasource.jndi-name")) {
            return new JndiDataSourceLookup()
                .getDataSource(env.getProperty("config.jdbc.jndi-name"));
        } else if (env.containsProperty("spring.datasource.url")) {
            return buildDatasource(
                env.getProperty("spring.datasource.url") + "/" +
                    env.getProperty("spring.datasource.database"),
                env.getProperty("spring.datasource.username"),
                env.getProperty("spring.datasource.password"),
                env.getProperty("spring.datasource.driver-class-name", "org.postgresql.Driver"));
        }
        return null;
    }

    @Primary
    @Bean
    public PlatformTransactionManager defaultTransactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());

        return transactionManager;
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
        return new PersistenceExceptionTranslationPostProcessor();
    }

    private Properties additionalProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto",
            env.getProperty("spring.jpa.hibernate.ddl-auto", "update"));
        properties.setProperty("hibernate.dialect",
            env.getProperty("spring.jpa.properties.hibernate.dialect",
                "org.hibernate.dialect.PostgreSQLDialect"));
        properties.setProperty("hibernate.jdbc.lob.non_contextual_creation",
            env.getProperty("spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation",
                Constants.TRUE));

        return properties;
    }

    public static DataSource buildDatasource(String url, String username, String password,
        String driver) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driver);
        return dataSource;
    }

    @Bean(name = "defaultObjectMapper")
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.build();
        objectMapper.registerModule(
            new Hibernate5Module().disable(Hibernate5Module.Feature.USE_TRANSIENT_ANNOTATION));
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return objectMapper;
    }
}
