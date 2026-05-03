package com.payprosys.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ServiceLoggingAspect {

    @Around("execution(* com.payprosys.service..*.*(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        log.info("{} START", methodName);
        try {
            Object result = joinPoint.proceed();
            log.info("{} END", methodName);
            return result;
        } catch (Throwable t) {
            log.info("{} END (exception)", methodName);
            throw t;
        }
    }
}
