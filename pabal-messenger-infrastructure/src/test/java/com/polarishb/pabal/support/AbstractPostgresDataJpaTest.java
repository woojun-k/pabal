package com.polarishb.pabal.support;

import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.MessageEntity;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.write.MessageWriteJpaRepository;
import com.polarishb.pabal.messenger.infrastructure.persistence.write.DirectChatMappingWriteRepositoryImpl;
import com.polarishb.pabal.messenger.infrastructure.persistence.write.MessageWriteRepositoryImpl;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.nio.file.Files;
import java.nio.file.Path;

@DataJpaTest(
        showSql = false,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.sql.init.mode=never"
        }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@ContextConfiguration(classes = AbstractPostgresDataJpaTest.PersistenceTestApplication.class)
public abstract class AbstractPostgresDataJpaTest {

    private static final PostgreSQLContainer<?> POSTGRES = startPostgres();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.locations", AbstractPostgresDataJpaTest::flywayMigrationLocation);
    }

    private static String flywayMigrationLocation() {
        Path workingDirectory = Path.of("").toAbsolutePath();
        Path fromRoot = workingDirectory.resolve("pabal-app/src/main/resources/db/migration");
        Path fromModule = workingDirectory.resolve("../pabal-app/src/main/resources/db/migration").normalize();

        Path migrationPath = Files.isDirectory(fromRoot) ? fromRoot : fromModule;
        return "filesystem:" + migrationPath;
    }

    private static PostgreSQLContainer<?> startPostgres() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:18.3")
                .withDatabaseName("pabal_test")
                .withUsername("test")
                .withPassword("test");
        container.start();
        Runtime.getRuntime().addShutdownHook(new Thread(
                container::close,
                "pabal-postgres-testcontainer-shutdown"
        ));
        return container;
    }

    @SpringBootConfiguration
    @EntityScan(basePackageClasses = MessageEntity.class)
    @EnableJpaRepositories(basePackageClasses = MessageWriteJpaRepository.class)
    @Import({
            DirectChatMappingWriteRepositoryImpl.class,
            MessageWriteRepositoryImpl.class
    })
    static class PersistenceTestApplication {
    }
}
