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

package org.eventa.core.config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.eventa.core.registry.LeaderHandlerRegistry;
import org.eventa.core.registry.NotLeaderHandlerRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class LeaderLatchConfiguration {

    private final EventaProperties eventaProperties;
    private final LeaderHandlerRegistry leaderHandlerRegistry;
    private final NotLeaderHandlerRegistry notLeaderHandlerRegistry;
    private final Map<Class<?>, Object> beanCache = new HashMap<>();
    private final Map<Class<?>, Method> methodCache = new HashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public LeaderLatchConfiguration(EventaProperties eventaProperties,
                                    LeaderHandlerRegistry leaderHandlerRegistry,
                                    NotLeaderHandlerRegistry notLeaderHandlerRegistry) {
        this.eventaProperties = eventaProperties;
        this.leaderHandlerRegistry = leaderHandlerRegistry;
        this.notLeaderHandlerRegistry = notLeaderHandlerRegistry;
    }

    @Bean(initMethod = "start", destroyMethod = "close")
    @ConditionalOnMissingBean
    public CuratorFramework curatorFramework() {
        return CuratorFrameworkFactory.newClient(eventaProperties.getCurator().getHostname(),
                new ExponentialBackoffRetry(eventaProperties.getCurator().getBaseSleepTimeMs(),
                        eventaProperties.getCurator().getMaxRetries()));
    }

    @Bean
    @ConditionalOnMissingBean
    public LeaderLatch leaderLatch(CuratorFramework curatorFramework, ApplicationContext applicationContext) {
        LeaderLatch leaderLatch = new LeaderLatch(curatorFramework, "/leader/latch");
        leaderLatch.addListener(new LeaderLatchListener() {
            @Override
            public void isLeader() {
                handleLeaderChange(true, applicationContext);
            }

            @Override
            public void notLeader() {
                handleLeaderChange(false, applicationContext);
            }
        }, eventaTaskExecutor());

        try {
            leaderLatch.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return leaderLatch;
    }

    private void handleLeaderChange(boolean isLeader, ApplicationContext applicationContext) {
        if (isLeader) {
            for (Class<?> handlerClass : leaderHandlerRegistry.getRegisteredClasses()) {
                executorService.submit(() -> {
                    try {
                        Object bean = beanCache.computeIfAbsent(handlerClass, applicationContext::getBean);
                        Method handlerMethod = methodCache.computeIfAbsent(handlerClass, leaderHandlerRegistry::getHandler);
                        handlerMethod.invoke(bean);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException("Failed to invoke handler method", e);
                    }
                });
            }
        } else {
            for (Class<?> handlerClass : notLeaderHandlerRegistry.getRegisteredClasses()) {
                executorService.submit(() -> {
                    try {
                        Object bean = beanCache.computeIfAbsent(handlerClass, applicationContext::getBean);
                        Method handlerMethod = methodCache.computeIfAbsent(handlerClass, notLeaderHandlerRegistry::getHandler);
                        handlerMethod.invoke(bean);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException("Failed to invoke handler method", e);
                    }
                });
            }
        }
    }

    private ThreadPoolTaskExecutor eventaTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("EventaTaskExecutor-");
        executor.initialize();
        return executor;
    }
}
