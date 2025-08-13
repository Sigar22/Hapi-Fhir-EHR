package ca.uhn.fhir.jpa.starter.custom;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
// import org.springframework.boot.context.properties.ConfigurationProperties; // Не нужен, так как JpaProperties внедряются
// import org.springframework.boot.jdbc.DataSourceBuilder; // Не нужен, так как DataSource внедряется
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource; // Используем javax.sql.DataSource
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
	basePackages = "ca.uhn.fhir.jpa.starter.custom", // Пакет, где находятся ваши репозитории
	entityManagerFactoryRef = "customEntityManagerFactory", // Ссылка на ваш EntityManagerFactory
	transactionManagerRef = "customTransactionManager" // Ссылка на ваш TransactionManager
)
@EntityScan("ca.uhn.fhir.jpa.starter.custom") // Указываем, где находятся ваши сущности
public class CustomJpaConfig {

	private final DataSource dataSource;
	private final JpaProperties jpaProperties; // Убедитесь, что это final

	// Единственный конструктор, внедряющий обе зависимости
	public CustomJpaConfig(DataSource dataSource, JpaProperties jpaProperties) {
		this.dataSource = dataSource;
		this.jpaProperties = jpaProperties;
	}

	// Удален метод customJpaProperties(), так как JpaProperties теперь внедряются
	// Удален метод customDataSource(), так как DataSource теперь внедряется

	// EntityManagerFactory для вашего кастомного модуля
	@Bean(name = "customEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean customEntityManagerFactory() {
		LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setDataSource(dataSource); // Используем внедренный dataSource
		em.setPackagesToScan("ca.uhn.fhir.jpa.starter.custom"); // Пакет, где находятся ваши сущности
		em.setPersistenceUnitName("CUSTOM_PU"); // Имя из persistence.xml
		em.setJpaVendorAdapter(new HibernateJpaVendorAdapter()); // Используем Hibernate

		Map<String, Object> properties = new HashMap<>(jpaProperties.getProperties()); // Используем внедренные jpaProperties
		// Можно добавить/переопределить специфичные свойства Hibernate здесь, если нужно
		// properties.put("hibernate.hbm2ddl.auto", "update"); // Если хотите, чтобы этот PU управлял своей схемой
		// properties.put("hibernate.dialect", "ca.uhn.fhir.jpa.model.dialect.HapiFhirPostgresDialect");
		em.setJpaPropertyMap(properties);

		return em;
	}

	// TransactionManager для вашего кастомного модуля
	@Bean(name = "customTransactionManager")
	public PlatformTransactionManager customTransactionManager(
		@Qualifier("customEntityManagerFactory") EntityManagerFactory customEntityManagerFactory) {
		return new JpaTransactionManager(customEntityManagerFactory);
	}
}