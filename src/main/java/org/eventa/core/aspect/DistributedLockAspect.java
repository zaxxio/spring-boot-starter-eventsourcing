/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2024 Partha Sutradhar.
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

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
