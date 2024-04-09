package org.openmbee.mms.core.services;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GenericServiceFactory implements ApplicationContextAware {

    private ApplicationContext context;
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }

    public <T> T getService(Class<T> clazz) {
        try {
            Map<String, T> beans = context.getBeansOfType(clazz);
            if(beans.size() > 0) {
                return beans.entrySet().iterator().next().getValue();
            } 
        } catch (BeansException e) {
            logger.error("Error getting service for Class : "+ clazz.getName(), e.getMessage());
        }
        return null;
    }

    public <T> T getServiceForSchema(Class<T> clazz, String schema) {
        if (clazz == null && schema == null) {
            return null;
        }

        //Try to find a matching service
        try {
            Map<String, T> beans = context.getBeansOfType(clazz);
            List<Map.Entry<String,T>> matches = beans.entrySet().stream()
                .filter(v -> v.getKey().startsWith(schema)).collect(Collectors.toList());
            if(matches.size() >= 1) {
                return matches.get(0).getValue();
            }
        } catch (BeansException e) {
            if (clazz != null) {
                logger.error("Error getting service for Class : " + clazz.getName());
            } else {
                logger.error("Error getting service for Class : class was null");
            }
        }
        return null;
    }

}