package com.diy.framework.web.server;

import com.diy.framework.web.bean.BeanContainer;
import com.diy.framework.web.bean.annotation.Controller;
import com.diy.framework.web.controller.AbstractController;

import java.util.HashMap;
import java.util.Map;

public class HandlerMapping {
    private final Map<String, AbstractController> map = new HashMap<>();

    public HandlerMapping(BeanContainer container) {
        for (AbstractController controller : container.getBeansOfType(AbstractController.class)) {
            Controller annotation = controller.getClass().getAnnotation(Controller.class);
            if (annotation == null) {
                continue;
            }
            String url = annotation.value();
            if (map.containsKey(url)) {
                throw new IllegalStateException("URL conflict: " + url);
            }
            map.put(url, controller);
        }
    }

    public AbstractController getController(String path) {
        if (!map.containsKey(path)) {
            throw new IllegalArgumentException("wrong context");
        }
        return map.get(path);
    }
}
