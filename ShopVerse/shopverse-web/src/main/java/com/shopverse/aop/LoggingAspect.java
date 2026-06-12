package com.shopverse.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Ch07-08: AOP logging aspect — cross-cutting concern for all use cases.
 * @Around intercepts call, measures duration, logs on entry/exit/error.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("execution(* com.shopverse.application.usecase..*(..))")
    public void useCaseLayer() {}

    @Pointcut("execution(* com.shopverse.web.controller..*(..))")
    public void controllerLayer() {}

    @Around("useCaseLayer() || controllerLayer()")
    public Object logExecution(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().toShortString();
        long start = System.currentTimeMillis();
        log.debug("→ Entering {}", method);
        try {
            Object result = pjp.proceed();
            log.debug("← Exiting {} in {}ms", method, System.currentTimeMillis() - start);
            return result;
        } catch (Exception ex) {
            log.error("✗ Exception in {} after {}ms: {}", method,
                      System.currentTimeMillis() - start, ex.getMessage());
            throw ex;
        }
    }
}
