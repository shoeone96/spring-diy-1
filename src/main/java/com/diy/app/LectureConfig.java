package com.diy.app;

import com.diy.framework.web.bean.annotation.Bean;
import com.diy.framework.web.bean.annotation.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class LectureConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
