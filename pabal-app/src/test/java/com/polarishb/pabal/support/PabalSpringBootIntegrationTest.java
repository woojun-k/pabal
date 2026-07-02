package com.polarishb.pabal.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(TYPE)
@Retention(RUNTIME)
@SpringBootTest
@ActiveProfiles("test")
@Import({
        PostgresTestConfiguration.class,
        RedisTestConfiguration.class,
        OtelCollectorTestConfiguration.class
})
public @interface PabalSpringBootIntegrationTest {

    @AliasFor(annotation = SpringBootTest.class, attribute = "classes")
    Class<?>[] classes() default {};

    @AliasFor(annotation = SpringBootTest.class, attribute = "properties")
    String[] properties() default {};

    @AliasFor(annotation = SpringBootTest.class, attribute = "webEnvironment")
    SpringBootTest.WebEnvironment webEnvironment() default SpringBootTest.WebEnvironment.MOCK;
}
