package com.fileservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.server.EnableRedisWebSession;
import org.springframework.web.server.session.HeaderWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;

@EnableRedisWebSession(redisNamespace = "filesharing")
@Configuration
public class SessionConfig {

    @Bean
    public LettuceConnectionFactory redisConnection() {
        return new LettuceConnectionFactory();
    }

    @Bean
    public WebSessionIdResolver headerSessionIdResolver(){
        HeaderWebSessionIdResolver resolver = new HeaderWebSessionIdResolver();
        resolver.setHeaderName("SESSION");
        return resolver;
    }
}
