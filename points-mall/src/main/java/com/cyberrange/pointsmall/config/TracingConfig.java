package com.cyberrange.pointsmall.config;

import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracerFactory;
import io.opentracing.util.GlobalTracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfig {

    @Bean
    public Tracer tracer() {
        Tracer tracer = NoopTracerFactory.create();
        GlobalTracer.registerIfAbsent(tracer);
        return tracer;
    }
}
