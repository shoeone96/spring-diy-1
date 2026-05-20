package com.diy.framework.web.bean;

import com.diy.framework.web.bean.annotation.Autowired;
import com.diy.framework.web.bean.annotation.Bean;
import com.diy.framework.web.bean.annotation.Component;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BeanContainer {

    private final BeanScanner beanScanner;
    private final List<BeanDefinition> definitions = new ArrayList<>();
    private final Map<String, Object> container = new ConcurrentHashMap<>();
    private final Set<String> inProgress = new HashSet<>();

    public BeanContainer() {
        this.beanScanner = new BeanScanner();
        initialize();
    }

    private void initialize() {
        // Pass 1: 정의 수집
        Set<Class<?>> componentClasses = beanScanner.scanClassesTypeAnnotatedWith(Component.class);
        for (Class<?> clazz : componentClasses) {
            registerDefinition(new AnnotatedBeanDefinition(clazz));

            List<Method> beanMethods = beanScanner.scanMethodsAnnotatedWith(clazz, Bean.class);
            for (Method method : beanMethods) {
                registerDefinition(new ConfigurationBeanDefinition(clazz.getSimpleName(), method));
            }
        }

        // Pass 2: 인스턴스화
        for (BeanDefinition def : new ArrayList<>(definitions)) {
            resolveBean(def.getBeanName());
        }
    }

    private void registerDefinition(BeanDefinition def) {
        boolean nameExists = definitions.stream()
                .anyMatch(d -> d.getBeanName().equals(def.getBeanName()));
        if (nameExists) {
            throw new RuntimeException("duplicate bean name: " + def.getBeanName());
        }
        definitions.add(def);
    }

    public Object resolveBean(String name) {
        Object existing = container.get(name);
        if (existing != null) {
            return existing;
        }

        if (inProgress.contains(name)) {
            throw new RuntimeException("circular dependency: " + name);
        }

        BeanDefinition def = definitions.stream()
                .filter(d -> d.getBeanName().equals(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("no definition for bean name: " + name));

        inProgress.add(name);
        try {
            Object instance = def.createInstance(this);
            container.put(name, instance);
            return instance;
        } finally {
            inProgress.remove(name);
        }
    }

    public Object resolveBean(Class<?> type) {
        List<Object> existing = container.values().stream()
                .filter(type::isInstance)
                .toList();
        if (existing.size() == 1) {
            return existing.getFirst();
        }
        if (existing.size() > 1) {
            throw new RuntimeException("more than two bean candidates for type: " + type);
        }

        List<BeanDefinition> matched = definitions.stream()
                .filter(d -> type.isAssignableFrom(d.getBeanType()))
                .toList();
        if (matched.isEmpty()) {
            throw new RuntimeException("no bean of type: " + type);
        }
        if (matched.size() > 1) {
            throw new RuntimeException("more than two bean candidates for type: " + type);
        }
        return resolveBean(matched.getFirst().getBeanName());
    }

    public Object[] resolveArguments(Executable executable) {
        Parameter[] parameters = executable.getParameters();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter p = parameters[i];
            if (p.isAnnotationPresent(Autowired.class)) {
                args[i] = resolveBean(p.getAnnotation(Autowired.class).beanValue());
            } else {
                args[i] = resolveBean(p.getType());
            }
        }
        return args;
    }

    public <T> T getBean(Class<T> clazz) {
        return findBean(clazz)
                .orElseThrow(() -> new RuntimeException("no matched Bean Found class: " + clazz));
    }

    public Object getBean(String name) {
        return findBean(name)
                .orElseThrow(() -> new RuntimeException("no matched Bean Found name: " + name));
    }

    public <T> Optional<T> findBean(Class<T> clazz) {
        List<T> beanList = container.values()
                .stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .toList();

        if (beanList.isEmpty()) {
            return Optional.empty();
        }

        if (beanList.size() == 1) {
            return Optional.of(beanList.getFirst());
        }

        throw new RuntimeException("more than two bean candidates are found");
    }

    public Optional<Object> findBean(String name) {
        Object bean = container.get(name);
        if (Objects.isNull(bean)) {
            return Optional.empty();
        }
        return Optional.of(bean);
    }

    public <T> List<T> getBeansOfType(Class<T> type) {
        return container.values()
                .stream()
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }
}
