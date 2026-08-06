package ru.yandex.practicum.configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.Driver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Slf4j
@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = {
        "ru.yandex.practicum.service",
        "ru.yandex.practicum.repository",
        "ru.yandex.practicum.mapper"
})
@PropertySource("classpath:application.properties")
public class DataSourceConfiguration {

    // Настройка DataSource — компонент, отвечающий за соединение с базой данных
    @Bean
    public DataSource dataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password
    ) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(Driver.class.getName());
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);

        // Максимальное количество соединений в пуле
        config.setMaximumPoolSize(10);
        // Минимальное количество «простаивающих» соединений, которые всегда должны быть готовы к использованию
        config.setMinimumIdle(5);
        // Таймаут ожидания соединения из пула: если за 30 секунд не удалось получить соединение — будет выброшено исключение
        config.setConnectionTimeout(30000);
        // Время простоя соединения, после которого оно может быть закрыто (не менее idleTimeout)
        config.setIdleTimeout(600000);
        // Максимальный срок жизни соединения: даже если оно активно используется, по истечении времени оно будет заменено новым
        config.setMaxLifetime(1800000);

        // Включаем кэширование подготовленных выражений (PreparedStatement) на стороне клиента - это ускоряет повторяющиеся запросы
        config.addDataSourceProperty("cachePrepStmts", "true");
        // Размер кэша подготовленных выражений
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        // Максимальная длина SQL-запроса, который может быть закэширован
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        // Имя пула соединений для отображения в логах
        config.setPoolName("HikariPool-my-blog-back-app");

        return new HikariDataSource(config);
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    // После инициализации контекста выполняем наполнение схемы базы данных
    @EventListener
    public void populate(ContextRefreshedEvent event) {
        DataSource dataSource = event.getApplicationContext().getBean(DataSource.class);
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();

        // Получаем путь к скрипту из свойств в зависимости от окружения
        Environment environment = event.getApplicationContext().getEnvironment();
        String scriptPath = environment.getProperty(
                "app.db.schema-script", "schema.sql");

        populator.addScript(new ClassPathResource(scriptPath));

        log.info("Executing schema script: " + scriptPath);
        populator.execute(dataSource);
    }

}
