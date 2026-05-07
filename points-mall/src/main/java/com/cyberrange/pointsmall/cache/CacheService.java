package com.cyberrange.pointsmall.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Optional;
import java.util.Set;

@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);
    private static final String KEY_PREFIX = "points_mall:";

    @Autowired
    private JedisPool jedisPool;

    @Autowired
    private ObjectMapper objectMapper;

    public void set(String key, Object value, int expireSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = objectMapper.writeValueAsString(value);
            jedis.setex(KEY_PREFIX + key, expireSeconds, json);
        } catch (Exception e) {
            log.warn("Cache set failed for key: {}", key, e);
        }
    }

    public <T> Optional<T> get(String key, Class<T> clazz) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get(KEY_PREFIX + key);
            if (json == null) return Optional.empty();
            return Optional.of(objectMapper.readValue(json, clazz));
        } catch (Exception e) {
            log.warn("Cache get failed for key: {}", key, e);
            return Optional.empty();
        }
    }

    public void delete(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(KEY_PREFIX + key);
        } catch (Exception e) {
            log.warn("Cache delete failed for key: {}", key, e);
        }
    }

    public void deleteByPattern(String pattern) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys(KEY_PREFIX + pattern);
            if (!keys.isEmpty()) {
                jedis.del(keys.toArray(new String[0]));
            }
        } catch (Exception e) {
            log.warn("Cache deleteByPattern failed for pattern: {}", pattern, e);
        }
    }

    public Long increment(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.incr(KEY_PREFIX + key);
        } catch (Exception e) {
            log.warn("Cache increment failed for key: {}", key, e);
            return null;
        }
    }

    public boolean setNx(String key, String value, int expireSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            String result = jedis.set(KEY_PREFIX + key, value, redis.clients.jedis.params.SetParams.setParams().nx().ex(expireSeconds));
            return "OK".equals(result);
        } catch (Exception e) {
            log.warn("Cache setNx failed for key: {}", key, e);
            return false;
        }
    }

    public boolean exists(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists(KEY_PREFIX + key);
        } catch (Exception e) {
            log.warn("Cache exists failed for key: {}", key, e);
            return false;
        }
    }

    public void hset(String key, String field, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(KEY_PREFIX + key, field, value);
        } catch (Exception e) {
            log.warn("Cache hset failed for key: {}", key, e);
        }
    }

    public String hget(String key, String field) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hget(KEY_PREFIX + key, field);
        } catch (Exception e) {
            log.warn("Cache hget failed for key: {}", key, e);
            return null;
        }
    }
}
