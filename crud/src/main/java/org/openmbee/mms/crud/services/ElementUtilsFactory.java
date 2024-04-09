package org.openmbee.mms.crud.services;

import org.openmbee.mms.core.utils.ElementUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ElementUtilsFactory implements ApplicationContextAware {
    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }

    public ElementUtils getElementUtil(String type) {
        try {
            ElementUtils eu = context.getBean(type + "Helper", ElementUtils.class);
            if (eu != null) {
                return eu;
            }
        } catch (BeansException e) {
        }
        return null;
    }
}