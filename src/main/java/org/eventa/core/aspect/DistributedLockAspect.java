package org.eventa.core.aspect;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.eventa.core.streotype.DistributedLock;
import org.springframework.stereotype.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Aspect
@Component
public class DistributedLockAspect {

    private static final Logger log = LogManager.getLogger(DistributedLockAspect.class);

    private final CuratorFramework curatorFramework;

    public DistributedLockAspect(CuratorFramework curatorFramework) {
        this.curatorFramework = curatorFramework;
    }

    @Around("@annotation(org.eventa.core.streotype.DistributedLock)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        DistributedLock distributedLock = signature.getMethod().getAnnotation(DistributedLock.class);
        String path = "/locks/" + distributedLock.value();
        InterProcessMutex lock = new InterProcessMutex(curatorFramework, path);

        log.info("Attempting to acquire lock: {}", path);

        if (lock.acquire(distributedLock.timeout(), distributedLock.timeUnit())) {
            log.info("Lock acquired: {}", path);
            try {
                return joinPoint.proceed();
            } finally {
                lock.release();
                log.info("Lock released: {}", path);
            }
        } else {
            log.error("Could not acquire lock: {}", path);
            throw new RuntimeException("Could not acquire lock");
        }
    }
}
