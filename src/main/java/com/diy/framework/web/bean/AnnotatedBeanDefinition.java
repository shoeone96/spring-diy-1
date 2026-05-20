package com.diy.framework.web.bean;

import com.diy.framework.web.bean.annotation.Autowired;

import java.lang.reflect.Constructor;

public class AnnotatedBeanDefinition implements BeanDefinition {

    private final Class<?> beanClass;
    private final Constructor<?> constructor;

    public AnnotatedBeanDefinition(Class<?> beanClass) {
        this.beanClass = beanClass;
        this.constructor = findConstructor(beanClass);
    }

    @Override
    public String getBeanName() {
        return beanClass.getSimpleName();
    }

    @Override
    public Class<?> getBeanType() {
        return beanClass;
    }

    @Override
    public Object createInstance(BeanContainer container) {
        try {
            Object[] args = container.resolveArguments(constructor);
            return constructor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("failed to instantiate bean: " + beanClass, e);
        }
    }

    private Constructor<?> findConstructor(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();

        if (constructors.length == 1) {
            return constructors[0];
        }

        for (Constructor<?> c : constructors) {
            if (c.isAnnotationPresent(Autowired.class)) {
                return c;
            }
        }

        throw new RuntimeException("no appropriate constructor for: " + clazz);
    }
}
