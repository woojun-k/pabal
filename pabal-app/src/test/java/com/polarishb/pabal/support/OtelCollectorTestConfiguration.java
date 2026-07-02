package com.polarishb.pabal.support;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp.OtlpMetricsConnectionDetails;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.OtlpTracingConnectionDetails;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingConnectionDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@TestConfiguration(proxyBeanMethods = false)
public class OtelCollectorTestConfiguration {

    private static final int OTLP_GRPC_PORT = 4317;
    private static final int OTLP_HTTP_PORT = 4318;
    private static final int HEALTH_PORT = 13133;

    @Bean
    @ServiceConnection(
            name = "otel/opentelemetry-collector-contrib",
            type = {
                    OtlpMetricsConnectionDetails.class,
                    OtlpTracingConnectionDetails.class,
                    OtlpLoggingConnectionDetails.class
            }
    )
    GenericContainer<?> otelCollector() {
        @SuppressWarnings("resource")
        GenericContainer<?> container = new GenericContainer<>(
                DockerImageName.parse("otel/opentelemetry-collector-contrib:latest")
        );

        return container
                .withExposedPorts(OTLP_GRPC_PORT, OTLP_HTTP_PORT, HEALTH_PORT)
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("otel/otel-collector-test.yaml"),
                        "/etc/otelcol-contrib/config.yaml"
                )
                .withCommand("--config=/etc/otelcol-contrib/config.yaml")
                .waitingFor(Wait.forHttp("/").forPort(HEALTH_PORT));
    }
}
