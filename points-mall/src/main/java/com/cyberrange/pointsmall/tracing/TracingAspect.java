package com.cyberrange.pointsmall.tracing;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TracingAspect {

    private static final Logger log = LoggerFactory.getLogger(TracingAspect.class);

    @Autowired
    private Tracer tracer;

    @Around("execution(* com.cyberrange.pointsmall.service.*.*(..))")
    public Object traceServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String operationName = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName();

        Span span = tracer.buildSpan(operationName).start();
        try (Scope scope = tracer.scopeManager().activate(span)) {
            span.setTag("class", joinPoint.getSignature().getDeclaringTypeName());
            span.setTag("method", joinPoint.getSignature().getName());

            Object result = joinPoint.proceed();

            span.setTag("success", true);
            return result;
        } catch (Exception e) {
            span.setTag("error", true);
            span.setTag("error.message", e.getMessage());
            log.error("Service method {} failed: {}", operationName, e.getMessage());
            throw e;
        } finally {
            span.finish();
        }
    }
}
