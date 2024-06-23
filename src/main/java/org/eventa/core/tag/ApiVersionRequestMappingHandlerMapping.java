//package org.eventa.core.tag;
//
//import org.eventa.core.tag.ApiVersion;
//import org.springframework.core.annotation.AnnotatedElementUtils;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
//import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
//import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
//
//import java.lang.reflect.Method;
//
//import org.springframework.core.annotation.AnnotatedElementUtils;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
//import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
//import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
//
//import java.lang.reflect.Method;
//import java.util.logging.Logger;
//
//public class ApiVersionRequestMappingHandlerMapping extends RequestMappingHandlerMapping {
//
//    private static final Logger LOGGER = Logger.getLogger(ApiVersionRequestMappingHandlerMapping.class.getName());
//
//    @Override
//    protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
//        ApiVersion methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, ApiVersion.class);
//        ApiVersion classAnnotation = AnnotatedElementUtils.findMergedAnnotation(handler.getClass(), ApiVersion.class);
//
//        if (methodAnnotation != null || classAnnotation != null) {
//            String version = (methodAnnotation != null) ? methodAnnotation.value() : classAnnotation.value();
//            PatternsRequestCondition apiPattern = new PatternsRequestCondition("/api/" + version);
//            PatternsRequestCondition existingPatterns = mapping.getPatternsCondition();
//
//            if (existingPatterns != null) {
//                apiPattern = apiPattern.combine(existingPatterns);
//            }
//
//            mapping = new RequestMappingInfo(
//                    mapping.getName(),
//                    apiPattern,
//                    mapping.getMethodsCondition(),
//                    mapping.getParamsCondition(),
//                    mapping.getHeadersCondition(),
//                    mapping.getConsumesCondition(),
//                    mapping.getProducesCondition(),
//                    mapping.getCustomCondition());
//
//            LOGGER.info("Registered handler method with version: " + version + " and pattern: " + apiPattern);
//        }
//        super.registerHandlerMethod(handler, method, mapping);
//    }
//
//    @Override
//    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
//        RequestMappingInfo mapping = super.getMappingForMethod(method, handlerType);
//
//        if (mapping != null) {
//            ApiVersion methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, ApiVersion.class);
//            ApiVersion classAnnotation = AnnotatedElementUtils.findMergedAnnotation(handlerType, ApiVersion.class);
//
//            if (methodAnnotation != null || classAnnotation != null) {
//                String version = (methodAnnotation != null) ? methodAnnotation.value() : classAnnotation.value();
//                PatternsRequestCondition apiPattern = new PatternsRequestCondition("/api/" + version);
//                PatternsRequestCondition existingPatterns = mapping.getPatternsCondition();
//
//                if (existingPatterns != null) {
//                    apiPattern = apiPattern.combine(existingPatterns);
//                }
//
//                mapping = new RequestMappingInfo(
//                        mapping.getName(),
//                        apiPattern,
//                        mapping.getMethodsCondition(),
//                        mapping.getParamsCondition(),
//                        mapping.getHeadersCondition(),
//                        mapping.getConsumesCondition(),
//                        mapping.getProducesCondition(),
//                        mapping.getCustomCondition());
//
//                LOGGER.info("Generated mapping for method with version: " + version + " and pattern: " + apiPattern);
//            }
//        }
//        return mapping;
//    }
//}
