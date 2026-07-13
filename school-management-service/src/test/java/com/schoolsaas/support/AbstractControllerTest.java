package com.schoolsaas.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.common.exception.GlobalExceptionHandler;
import com.schoolsaas.config.security.JwtAuthenticationEntryPoint;
import com.schoolsaas.config.security.JwtAuthenticationFilter;
import com.schoolsaas.config.security.JwtService;
import com.schoolsaas.config.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        GlobalExceptionHandler.class
})
public abstract class AbstractControllerTest {

    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    protected JwtService jwtService;

    protected String asJson(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }
}
