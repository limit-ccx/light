package com.zhiguang.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

/**
 * 数据源切换切面
 * <p>
 * - @Transactional(readOnly = true) 的方法自动切换到 replica
 * - 其他方法默认使用 master
 * - Order 设置为 -1，确保在事务切面之前执行
 */
@Slf4j
@Aspect
@Component
@Order(-1)
public class DataSourceAspect {

    /**
     * 切面：根据方法上的 @Transactional 注解决定数据源
     */
    @Around("@within(org.springframework.transaction.annotation.Transactional) || " +
            "@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object aroundTransactionalMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法上的 @Transactional 注解
        Transactional transactional = getTransactionalAnnotation(joinPoint);

        // 暂时禁用从库路由：从库尚未配置主从复制，缺少数据
        // 所有操作先走主库，避免查询失败
        DataSourceConfig.DataSourceContextHolder.useMaster();
        log.debug("统一使用主库: {}", joinPoint.getSignature());

        try {
            return joinPoint.proceed();
        } finally {
            // 清除数据源上下文
            DataSourceConfig.DataSourceContextHolder.clearDataSourceType();
        }
    }

    /**
     * 获取方法上的 @Transactional 注解
     */
    private Transactional getTransactionalAnnotation(ProceedingJoinPoint joinPoint) {
        // 先从方法上查找
        Method method = getMethod(joinPoint);
        if (method != null) {
            Transactional transactional = method.getAnnotation(Transactional.class);
            if (transactional != null) {
                return transactional;
            }
        }

        // 再从类上查找
        Class<?> targetClass = joinPoint.getTarget().getClass();
        return targetClass.getAnnotation(Transactional.class);
    }

    /**
     * 获取当前方法
     */
    private Method getMethod(ProceedingJoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        Class<?> targetClass = joinPoint.getTarget().getClass();
        Class<?>[] parameterTypes = getParameterTypes(joinPoint);

        try {
            return targetClass.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            log.warn("无法获取方法: {} in {}", methodName, targetClass);
            return null;
        }
    }

    /**
     * 获取参数类型数组
     */
    private Class<?>[] getParameterTypes(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        Class<?>[] parameterTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
        }
        return parameterTypes;
    }
}