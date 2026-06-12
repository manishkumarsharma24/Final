package com.shopverse.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ch16-05: OpenTelemetry / Micrometer Observation config.
 * Enables @Observed annotation on any bean for automatic tracing + metrics.
 * Traces exported to Jaeger/Zipkin via micrometer-tracing-bridge-otel.
 */
@Configuration
public class ObservabilityConfig {

    /**
     * Enables @Observed support — wraps method calls with spans.
     */
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry registry) {
        return new ObservedAspect(registry);
    }
}
