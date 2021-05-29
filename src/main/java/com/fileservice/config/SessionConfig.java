package com.fileservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.server.EnableRedisWebSession;

@EnableRedisWebSession(redisNamespace = "filesharing")
@Configuration
public class SessionConfig {

    @Bean
    public LettuceConnectionFactory redisConnection() {
        return new LettuceConnectionFactory();
    }
}
