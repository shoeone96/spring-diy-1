package com.diy.framework.web.bean;

import java.lang.reflect.Method;

public class ConfigurationBeanDefinition implements BeanDefinition {

    private final String factoryBeanName;
    private final Method factoryMethod;

    public ConfigurationBeanDefinition(String factoryBeanName, Method factoryMethod) {
        this.factoryBeanName = factoryBeanName;
        this.factoryMethod = factoryMethod;
    }

    @Override
    public String getBeanName() {
        return factoryMethod.getName();
    }

    @Override
    public Class<?> getBeanType() {
        return factoryMethod.getReturnType();
    }

    @Override
    public Object createInstance(BeanContainer container) {
        try {
            Object factoryBean = container.resolveBean(factoryBeanName);
            Object[] args = container.resolveArguments(factoryMethod);
            return factoryMethod.invoke(factoryBean, args);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("failed to invoke factory method: " + factoryMethod, e);
        }
    }
}
