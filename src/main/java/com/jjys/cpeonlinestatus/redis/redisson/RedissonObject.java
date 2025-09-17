package com.jjys.cpeonlinestatus.redis.redisson;

import com.jjys.cpeonlinestatus.redis.config.properties.RedissonProperties;
import com.jjys.cpeonlinestatus.redis.function.DataCache;
import com.jjys.cpeonlinestatus.redis.function.RealData;
import jakarta.annotation.Resource;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.concurrent.TimeUnit;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RedissonObject {
    @Resource
    private RedissonClient redissonClient;

    @Resource
    private RedissonProperties redissonProperties;

    public <T> T getValue(String name) {
        RBucket<T> bucket = this.redissonClient.getBucket(name);
        return (T) bucket.get();
    }

    public <T> T getValue(String name, RealData<T> realData) {
        return getValue(name, realData, this.redissonProperties.getDataValidTime());
    }

    public <T> T getValue(String name, RealData<T> realData, Long time) {
        T value = getValue(name);
        if (value == null) {
            value = (T) realData.get();
            if (ObjectUtils.isEmpty(value)) {
                delete(name);
            } else {
                setValue(name, value, time);
            }
        }
        return value;
    }

    public <T> T getValue(String name, RealData<T> realData, DataCache<T> dataCache, Long time) {
        T value = getValue(name);
        if (value == null) {
            value = (T) realData.get();
            if (ObjectUtils.isEmpty(value)) {
                delete(name);
            } else {
                Boolean cache = dataCache.isCache(value);
                if (cache.booleanValue())
                    setValue(name, value, time);
            }
        }
        return value;
    }

    public <T> RBucket<T> getBucket(String name) {
        return this.redissonClient.getBucket(name);
    }

    public <T> void setValue(String name, T value) {
        setValue(name, value, this.redissonProperties.getDataValidTime());
    }

    public <T> void setValue(String name, T value, Long time) {
        RBucket<Object> bucket = this.redissonClient.getBucket(name);
        if (time.longValue() == -1L) {
            bucket.set(value);
        } else {
            bucket.set(value, time.longValue(), TimeUnit.MILLISECONDS);
        }
    }

    public <T> Boolean trySetValue(String name, T value, Long time) {
        boolean b;
        RBucket<Object> bucket = this.redissonClient.getBucket(name);
        if (time.longValue() == -1L) {
            b = bucket.trySet(value);
        } else {
            b = bucket.trySet(value, time.longValue(), TimeUnit.MILLISECONDS);
        }
        return Boolean.valueOf(b);
    }

    public <T> Boolean trySetValue(String name, T value) {
        return trySetValue(name, value, this.redissonProperties.getDataValidTime());
    }

    public Boolean delete(String name) {
        return Boolean.valueOf(this.redissonClient.getBucket(name).delete());
    }
}
