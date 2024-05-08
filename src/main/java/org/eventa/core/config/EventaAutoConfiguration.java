package org.eventa.core.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.eventa.core.repository.EventStoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Log4j2
@AutoConfiguration
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(MongoAutoConfiguration.class)
@EnableConfigurationProperties(EventaProperties.class)
@EnableMongoRepositories(basePackageClasses = {EventStoreRepository.class})
@ComponentScan(basePackages = "org.eventa.core")
public class EventaAutoConfiguration {


    @Autowired
    private EventaProperties eventaProperties;

    @PostConstruct
    public void postConstruct() {
        System.out.println("Hello World!!" + eventaProperties.getMongoDbUsername());
    }

    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
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
