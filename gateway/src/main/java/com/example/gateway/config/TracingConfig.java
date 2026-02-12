package com.example.gateway.config;

import brave.Tracing;
import brave.handler.SpanHandler;
import brave.propagation.CurrentTraceContext;
import brave.propagation.StrictCurrentTraceContext;
import brave.sampler.Sampler;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfig {

    @Bean
    public Tracer micrometerTracer() {
        CurrentTraceContext braveCurrentTraceContext = StrictCurrentTraceContext.create();
        Tracing braveTracing = Tracing.newBuilder()
                .currentTraceContext(braveCurrentTraceContext)
                .sampler(Sampler.ALWAYS_SAMPLE)
                .addSpanHandler(SpanHandler.NOOP)
                .build();

        return new BraveTracer(
                braveTracing.tracer(),
                new BraveCurrentTraceContext(braveCurrentTraceContext),
                new BraveBaggageManager()
        );
    }
}
