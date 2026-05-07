package com.cyberrange.pointsmall.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class RedisConfig {

    @Value("${redis.host:localhost}")
    private String host;

    @Value("${redis.port:6379}")
    private int port;

    @Value("${redis.password:}")
    private String password;

    @Value("${redis.timeout:3000}")
    private int timeout;

    @Value("${redis.pool.max-active:10}")
    private int maxActive;

    @Value("${redis.pool.max-idle:5}")
    private int maxIdle;

    @Value("${redis.pool.min-idle:1}")
    private int minIdle;

    @Bean
    public JedisPool jedisPool() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxActive);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setTestOnBorrow(false);
        config.setTestOnReturn(false);
        config.setMinEvictableIdleTime(java.time.Duration.ofSeconds(60));
        config.setNumTestsPerEvictionRun(3);

        if (password != null && !password.isEmpty()) {
            return new JedisPool(config, host, port, timeout, password);
        }
        return new JedisPool(config, host, port, timeout);
    }
}
