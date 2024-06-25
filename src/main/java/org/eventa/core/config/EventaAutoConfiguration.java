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

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.eventa.core.interceptor.CommandInterceptorRegisterer;
import org.eventa.core.registry.*;
import org.eventa.core.repository.EventStoreRepository;
import org.eventa.core.repository.SagaStateRepository;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Log4j2
@AutoConfiguration
@Configuration(proxyBeanMethods = true)
@AutoConfigureAfter(MongoAutoConfiguration.class)
@EnableConfigurationProperties(EventaProperties.class)
@ConfigurationPropertiesScan
@EnableMongoRepositories(basePackageClasses = {EventStoreRepository.class, SagaStateRepository.class})
@ComponentScan(basePackages = "org.eventa.core")
@PropertySource("classpath:application.properties")
@RequiredArgsConstructor
@EnableAsync
public class EventaAutoConfiguration implements BeanFactoryAware {


    private final EventaProperties eventaProperties;
    private BeanFactory beanFactory;
    private final LeaderHandlerRegistry leaderHandlerRegistry;
    private final NotLeaderHandlerRegistry notLeaderHandlerRegistry;

    @PostConstruct
    public void postConstruct() {

    }


    @Bean(initMethod = "start", destroyMethod = "close")
    @ConditionalOnMissingBean
    public CuratorFramework curatorFramework() {
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient(eventaProperties.getCurator().getHostname(),
                new ExponentialBackoffRetry(eventaProperties.getCurator().getBaseSleepTimeMs(), eventaProperties.getCurator().getMaxRetries()));
        curatorFramework.start();
        try {
            curatorFramework.blockUntilConnected();
            if (curatorFramework.getZookeeperClient().isConnected()) {
                log.info("Connected to ZooKeeper");
            } else {
                throw new RuntimeException("Failed to connect to ZooKeeper");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to connect to Zookeeper", e);
        }
        return curatorFramework;
    }


    @Bean
    @ConditionalOnMissingBean
    public LeaderLatch leaderLatch(CuratorFramework curatorFramework, ApplicationContext applicationContext) {
        LeaderLatch leaderLatch = new LeaderLatch(curatorFramework, "/leader/latch");
        leaderLatch.addListener(new LeaderLatchListener() {
            @Override
            public void isLeader() {
                for (Class<?> handlerClass : leaderHandlerRegistry.getRegisteredClasses()) {
                    Method handlerMethod = leaderHandlerRegistry.getHandler(handlerClass);
                    try {
                        Object bean = applicationContext.getBean(handlerClass);
                        handlerMethod.invoke(bean);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException("Failed to invoke handler method", e);
                    }
                }
            }

            @Override
            public void notLeader() {
                for (Class<?> handlerClass : notLeaderHandlerRegistry.getRegisteredClasses()) {
                    Method handlerMethod = notLeaderHandlerRegistry.getHandler(handlerClass);
                    try {
                        Object bean = applicationContext.getBean(handlerClass);
                        handlerMethod.invoke(bean);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException("Failed to invoke handler method", e);
                    }
                }
            }
        }, new SimpleAsyncTaskExecutor());
        try {
            leaderLatch.start();
            leaderLatch.await(); // Ensure latch starts and awaits leadership decision
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return leaderLatch;
    }


    @Bean
    public TaskExecutor eventaTaskExecutor() {
        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
        taskExecutor.setThreadNamePrefix("eventa-group");
        taskExecutor.setConcurrencyLimit(Runtime.getRuntime().availableProcessors() / 2);
        return taskExecutor;
    }


    @Bean
    @ConditionalOnMissingBean
    public CommandInterceptorRegisterer commandInterceptorRegisterer() {
        return new CommandInterceptorRegisterer();
    }

    @Bean
    @ConditionalOnMissingBean
    public MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }


    /*@Bean
    public SmartInstantiationAwareBeanPostProcessor customConstructorResolver() {
        return new SmartInstantiationAwareBeanPostProcessor() {
            @Override
            public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName) throws BeansException {
                // Check if the bean class is one of your aggregate roots
                if (AggregateRoot.class.isAssignableFrom(beanClass)) {
                    // Try to find a constructor that takes a BaseCommand subclass
                    for (Constructor<?> constructor : beanClass.getConstructors()) {
                        for (Class<?> paramType : constructor.getParameterTypes()) {
                            if (BaseCommand.class.isAssignableFrom(paramType)) {
                                return new Constructor<?>[]{constructor};
                            }
                        }
                    }
                }
                return null;
            }
        };
    }*/

}
