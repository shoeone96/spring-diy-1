package com.diy.framework.web.bean;

public interface BeanDefinition {

    String getBeanName();

    Class<?> getBeanType();

    Object createInstance(BeanContainer container);
}
