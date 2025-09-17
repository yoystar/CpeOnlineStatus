package com.jjys.cpeonlinestatus.redis.redisson;

import com.jjys.cpeonlinestatus.utils.SpringUtils;
import com.jjys.cpeonlinestatus.utils.StringUtils;
import com.jjys.cpeonlinestatus.redis.config.properties.RedissonProperties;
import com.jjys.cpeonlinestatus.redis.function.DataCache;
import com.jjys.cpeonlinestatus.redis.function.RealData;
import com.jjys.cpeonlinestatus.redis.function.RealDataMap;
import com.jjys.cpeonlinestatus.redis.function.RealDataSet;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.redisson.api.*;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * RedissonCollectionCache 工具类
 */

/**
 * RMap
 * RMap 和 RMapCache 区别：
 * 1. 过期机制：RMap 不支持过期，RMapCache 支持对整个Map或单个键值对设置TTL和最大空闲时间
 * 2. 内存占用：RMap 较低，RMapCache 较高（需要额外存储过期信息）
 * 3. 性能：RMap 略高，RMapCache 略低（需要处理过期逻辑）
 * 4. 使用场景：
 *    - RMap：适用于需要长期存储且不需要过期的数据，追求高性能
 *    - RMapCache：适用于需要缓存自动过期的数据，如会话管理、临时数据存储
 * 5. 淘汰策略：RMap 无淘汰策略，RMapCache 支持基于时间的自动淘汰
 */

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings(value = {"unchecked", "rawtypes"})
public class RedissonCollectionCache {

    private static final RedissonClient CLIENT = SpringUtils.getBean(RedissonClient.class);
    private static final RedissonProperties redissonProperties = SpringUtils.getBean(RedissonProperties.class);

    public <K, V> RMapCache<K, V> getMapCache(String name) {
        return CLIENT.getMapCache(name);
    }

    public <K, V> RMapCache<K, V> getMapCache(String name, RealDataMap<K, V> realDataMap, Long time) {
        RMapCache<Object, Object> map = CLIENT.getMapCache(name);
        if (map == null || map.size() == 0) {
            Map<K, V> objectObjectMap = realDataMap.get();
            setMapCacheValues(name, objectObjectMap, time);
        }
        return CLIENT.getMapCache(name);
    }

    public <T> T getMapCacheValue(String name, String key) {
        RMapCache<Object, T> mapCache = getMapCache(name);
        return (T) mapCache.get(key);
    }

    public <T> T getMapCacheValue(String name, String key, RealData<T> realData) {
        return getMapCacheValue(name, key, realData, this.redissonProperties.getDataValidTime());
    }

    public <T> T getMapCacheValue(String name, String key, RealData<T> realData, Long time) {
        RMapCache<Object, T> mapCache = getMapCache(name);
        T o = (T) mapCache.get(key);
        if (o == null) {
            o = (T) realData.get();
            if (ObjectUtils.isEmpty(o)) {
                mapCache.remove(key);
            } else {
                setMapCacheValue(name, key, o, time);
            }
        }
        return o;
    }

    public <T> T getMapCacheValue(String name, String key, RealData<T> realData, DataCache<T> dataCache, Long time) {
        RMapCache<Object, T> mapCache = getMapCache(name);
        T o = (T) mapCache.get(key);
        if (o == null) {
            o = (T) realData.get();
            if (ObjectUtils.isEmpty(o)) {
                mapCache.remove(key);
            } else {
                Boolean cache = dataCache.isCache(o);
                if (cache.booleanValue())
                    setMapCacheValue(name, key, o, time);
            }
        }
        return o;
    }

    public <K, V> void setMapCacheValues(String name, Map<K, V> data, Long time) {
        RMapCache<K, V> map = CLIENT.getMapCache(name);
        if (time == null)
            time = this.redissonProperties.getDataValidTime();
        map.putAll(data, time.longValue(), TimeUnit.MILLISECONDS);
    }

    public void setMapCacheValue(String name, String key, Object value, Long time) {
        setMapCacheValue(name, key, value, time, Long.valueOf(0L));
    }

    public <T> void setMapCacheValue(String name, String key, T value, Long time, Long maxIdleTime) {
        RMapCache<String, T> map = CLIENT.getMapCache(name);
        if (time == null)
            time = this.redissonProperties.getDataValidTime();
        map.put(key, value, time.longValue(), TimeUnit.MILLISECONDS, maxIdleTime.longValue(), TimeUnit.MILLISECONDS);
    }

    public <K, V> void setMapCacheValues(String name, Map<K, V> data) {
        setMapCacheValues(name, data, this.redissonProperties.getDataValidTime());
    }

    public void setMapCacheValue(String name, String key, Object value) {
        setMapCacheValue(name, key, value, this.redissonProperties.getDataValidTime());
    }

    public RListMultimapCache<String, String> getListMultimapCache(String cacheName) {
        return CLIENT.getListMultimapCache(cacheName);
    }
    public RList<String> getListMultimapCache(String cacheName, String key) {
        RListMultimapCache<String, String> map = getListMultimapCache(cacheName);
        return map.get(key);
    }

    public void setListMultimapCache(String cacheName, String key, String value, Long time) {
        RListMultimapCache<String, String> map = getListMultimapCache(cacheName);
        map.put(key, value);
        if (time == null) {
            map.expireKey(key, this.redissonProperties.getDataValidTime(), TimeUnit.MILLISECONDS);
        } else if (time.longValue() != -1L) {
            map.expireKey(key, time, TimeUnit.MILLISECONDS);
        }
    }

    public void setListMultimapCache(String cacheName, String key, String value) {
        RListMultimapCache<String, String> map = getListMultimapCache(cacheName);
        map.put(key, value) ;

        map.expireKey(key, 1, TimeUnit.HOURS);
    }

    public void clearListMultimapCache(String cacheName, String key) {
        RListMultimapCache<String, String> map = getListMultimapCache(cacheName);
        RList<String> list = map.get(key);
        list.clear();
    }

    public void clearListMultimapCache(String cacheName) {
        RListMultimapCache<String, String> map = getListMultimapCache(cacheName);
        map.clear();
    }

    public <T> RSetCache<T> getSetCache(String name) {
        return CLIENT.getSetCache(name);
    }

    public <T> RSetCache<T> getSetCache(String name, RealDataSet<T> realDataSet, Long time) {
        RSetCache<Object> set = getSetCache(name);
        if (set == null || set.size() == 0) {
            Set<T> objects = realDataSet.get();
            setSetCacheValues(name, objects, time);
        }
        return getSetCache(name);
    }

    public <T> RSetCache<T> getSetCache(String name, RealDataSet<T> realDataSet) {
        return getSetCache(name, realDataSet, this.redissonProperties.getDataValidTime());
    }

    public <T> void setSetCacheValues(String name, Set<T> data, Long time) {
        RSetCache<Object> set = CLIENT.getSetCache(name);
        set.addAll(data);
        Long dataValidTime = this.redissonProperties.getDataValidTime();
        if (time == null) {
            set.expire(Duration.ofMillis(dataValidTime.longValue()));
        } else if (time.longValue() != -1L) {
            set.expire(Duration.ofMillis(time.longValue()));
        }
    }

    public void setSetCacheValue(String name, Object data, Long time) {
        RSetCache<Object> set = CLIENT.getSetCache(name);
        if (time == null)
            time = this.redissonProperties.getDataValidTime();
        set.add(data, time.longValue(), TimeUnit.MILLISECONDS);
    }

    public <T> void setSetCacheValues(String name, Set<T> data) {
        setSetCacheValues(name, data, this.redissonProperties.getDataValidTime());
    }

    public void setSetValues(String name, Object data) {
        setSetCacheValue(name, data, this.redissonProperties.getDataValidTime());
    }

    /**
     * 删除指定缓存Map中的某个键值对
     *
     * @param name 缓存Map的名称
     * @param key  要删除的键
     * @return 如果键存在并且被成功删除，则返回true；否则返回false
     */
    public boolean deleteMapCacheValue(String name, String key) {
        if (StringUtils.isEmpty(name) || StringUtils.isEmpty(key)) {
            return false;
        }

        RMapCache<Object, Object> mapCache = getMapCache(name);
        return mapCache.remove(key) != null;
    }

    /**
     * 删除指定缓存Map中的多个键值对
     *
     * @param name 缓存Map的名称
     * @param keys 要删除的键集合
     * @return 成功删除的键的数量
     */
    public long deleteMapCacheValues(String name, Collection<String> keys) {
        if (StringUtils.isEmpty(name) || CollectionUtils.isEmpty(keys)) {
            return 0;
        }

        RMapCache<Object, Object> mapCache = getMapCache(name);
        long count = 0;
        for (String key : keys) {
            if (mapCache.remove(key) != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * 清空指定名称的缓存Map
     *
     * @param name 缓存Map的名称
     * @return 如果成功清空，则返回true
     */
    public boolean clearMapCache(String name) {
        if (StringUtils.isEmpty(name)) {
            return false;
        }

        RMapCache<Object, Object> mapCache = getMapCache(name);
        mapCache.clear();
        return true;
    }

    /**
     * 删除指定名称的缓存Map（完全删除该Map）
     *
     * @param name 缓存Map的名称
     * @return 如果成功删除，则返回true
     */
    public boolean deleteMapCache(String name) {
        if (StringUtils.isEmpty(name)) {
            return false;
        }

        RMapCache<Object, Object> mapCache = getMapCache(name);
        return mapCache.delete();
    }
}
