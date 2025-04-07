package org.springframework.samples.petclinic.visits.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.aop.TimedAspect;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = MetricConfigTest.TestConfig.class)
class MetricConfigTest {

    @Configuration
    static class TestConfig extends MetricConfig {
        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Test
    void shouldLoadTimedAspectBean() {
        MeterRegistry registry = new SimpleMeterRegistry();
        MetricConfig config = new MetricConfig();
        TimedAspect timedAspect = config.timedAspect(registry);

        assertThat(timedAspect).isNotNull();
    }
}