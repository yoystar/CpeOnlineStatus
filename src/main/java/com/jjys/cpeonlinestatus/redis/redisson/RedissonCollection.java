package com.jjys.cpeonlinestatus.redis.redisson;

import com.jjys.cpeonlinestatus.redis.config.properties.RedissonProperties;
import com.jjys.cpeonlinestatus.redis.function.*;
import com.jjys.cpeonlinestatus.utils.SpringUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RedissonCollection 工具类
 */

/**
 * RMap
 * RMap 和 RMapCache 区别：
 * 1. 过期机制：RMap 不支持过期，RMapCache 支持对整个Map或单个键值对设置TTL和最大空闲时间
 * 2. 内存占用：RMap 较低，RMapCache 较高（需要额外存储过期信息）
 * 3. 性能：RMap 略高，RMapCache 略低（需要处理过期逻辑）
 * 4. 使用场景：
 * - RMap：适用于需要长期存储且不需要过期的数据，追求高性能
 * - RMapCache：适用于需要缓存自动过期的数据，如会话管理、临时数据存储
 * 5. 淘汰策略：RMap 无淘汰策略，RMapCache 支持基于时间的自动淘汰
 */

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings(value = {"unchecked", "rawtypes"})
public class RedissonCollection {

    private static final RedissonClient CLIENT = SpringUtils.getBean(RedissonClient.class);
    private static final RedissonProperties redissonProperties = SpringUtils.getBean(RedissonProperties.class);

    public <K, V> RMap<K, V> getMap(String name) {
        return CLIENT.getMap(name);
    }

    public <K, V> RMap<K, V> getMap(String name, RealDataMap<K, V> realDataMap) {
        return getMap(name, realDataMap, this.redissonProperties.getDataValidTime());
    }

    public <K, V> RMap<K, V> getMap(String name, RealDataMap<K, V> realDataMap, Long time) {
        RMap<Object, Object> map = CLIENT.getMap(name);
        if (map == null || map.size() == 0) {
            Map<?, ?> objectObjectMap = realDataMap.get();
            setMapValues(name, objectObjectMap, time);
        }
        return CLIENT.getMap(name);
    }

    public <T> T getMapValue(String name, String key) {
        RMap<Object, T> map = getMap(name);
        return (T) map.get(key);
    }

    public <T> T getMapValue(String name, String key, RealData<T> realData) {
        return getMapValue(name, key, realData, this.redissonProperties.getDataValidTime());
    }

    public <T> T getMapValue(String name, String key, RealData<T> realData, Long time) {
        RMap<Object, T> map = getMap(name);
        T o = (T) map.get(key);
        if (o == null) {
            o = (T) realData.get();
            if (ObjectUtils.isEmpty(o)) {
                map.remove(key);
            } else {
                setMapValue(name, key, o, time);
            }
        }
        return o;
    }

    public <T> T getMapValue(String name, String key, RealData<T> realData, DataCache<T> dataCache, Long time) {
        RMap<Object, T> map = getMap(name);
        T o = (T) map.get(key);
        if (o == null) {
            o = (T) realData.get();
            if (ObjectUtils.isEmpty(o)) {
                map.remove(key);
            } else {
                Boolean cache = dataCache.isCache(o);
                if (cache.booleanValue())
                    setMapValue(name, key, o, time);
            }
        }
        return o;
    }

    public <K, V> void setMapValues(String name, Map<K, V> data, Long time) {
        RMap<K, V> map = CLIENT.getMap(name);
        map.putAll(data);
        Long dataValidTime = this.redissonProperties.getDataValidTime();
        if (time == null) {
            map.expire(Duration.ofMillis(dataValidTime.longValue()));
        } else if (time.longValue() != -1L) {
            map.expire(Duration.ofMillis(time.longValue()));
        }
    }

    public <T> void setMapValue(String name, String key, T value, Long time) {
        RMap<String, T> map = CLIENT.getMap(name);
        map.put(key, value);
        Long dataValidTime = this.redissonProperties.getDataValidTime();
        if (time == null) {
            map.expire(Duration.ofMillis(dataValidTime.longValue()));
        } else if (time.longValue() != -1L) {
            map.expire(Duration.ofMillis(time.longValue()));
        }
    }

    public <K, V> void setMapValues(String name, Map<K, V> data) {
        setMapValues(name, data, this.redissonProperties.getDataValidTime());
    }

    public void setMapValue(String name, String key, Object value) {
        setMapValue(name, key, value, this.redissonProperties.getDataValidTime());
    }

    public <T> RList<T> getList(String name) {
        return CLIENT.getList(name);
    }

    public <T> RList<T> getList(String name, RealDataList<T> realDataList, Long time) {
        RList<Object> list = getList(name);
        if (list == null || list.size() == 0) {
            List<T> objects = realDataList.get();
            setListValues(name, objects, time);
        }
        return getList(name);
    }

    public <T> RList<T> getList(String name, RealDataList<T> realDataList) {
        return getList(name, realDataList, this.redissonProperties.getDataValidTime());
    }

    public <T> T getListValue(String name, Integer index) {
        RList<T> list = getList(name);
        return (T) list.get(index.intValue());
    }

    public <T> void setListValues(String name, List<T> data, Long time) {
        RList<T> list = CLIENT.getList(name);
        list.addAll(data);
        Long dataValidTime = this.redissonProperties.getDataValidTime();
        if (time == null) {
            list.expire(Duration.ofMillis(dataValidTime.longValue()));
        } else if (time.longValue() != -1L) {
            list.expire(Duration.ofMillis(time.longValue()));
        }
    }

    public <T> void setListValue(String name, T data, Long time) {
        RList<T> list = CLIENT.getList(name);
        list.add(data);
        Long dataValidTime = this.redissonProperties.getDataValidTime();
        if (time == null) {
            list.expire(Duration.ofMillis(dataValidTime.longValue()));
        } else if (time.longValue() != -1L) {
            list.expire(Duration.ofMillis(time.longValue()));
        }
    }

    public <T> void setListValues(String name, List<T> data) {
        setListValues(name, data, this.redissonProperties.getDataValidTime());
    }

    public void setListValue(String name, Object data) {
        setListValue(name, data, this.redissonProperties.getDataValidTime());
    }

    public <T> RSet<T> getSet(String name) {
        return CLIENT.getSet(name);
    }

    public <T> RSet<T> getSet(String name, RealDataSet<T> realDataSet, Long time) {
        RSet<Object> set = getSet(name);
        if (set == null || set.size() == 0) {
            Set<T> objects = realDataSet.get();
            setSetValues(name, objects, time);
        }
        return getSet(name);
    }

    public <T> RSet<T> getSet(String name, RealDataSet<T> realDataSet) {
        return getSet(name, realDataSet, this.redissonProperties.getDataValidTime());
    }

    public <T> void setSetValues(String name, Set<T> data, Long time) {
        RSet<T> set = CLIENT.getSet(name);
        set.addAll(data);
        Long dataValidTime = this.redissonProperties.getDataValidTime();
        if (time == null) {
            set.expire(Duration.ofMillis(dataValidTime.longValue()));
        } else if (time.longValue() != -1L) {
            set.expire(Duration.ofMillis(time.longValue()));
        }
    }

    public <T> void setSetValue(String name, T data, Long time) {
        RSet<T> set = CLIENT.getSet(name);
        set.add(data);
        Long dataValidTime = this.redissonProperties.getDataValidTime();
        if (time == null) {
            set.expire(Duration.ofMillis(dataValidTime.longValue()));
        } else if (time.longValue() != -1L) {
            set.expire(Duration.ofMillis(time.longValue()));
        }
    }

    public <T> void setSetValues(String name, Set<T> data) {
        setSetValues(name, data, this.redissonProperties.getDataValidTime());
    }

    public void setSetValues(String name, Object data) {
        setSetValue(name, data, this.redissonProperties.getDataValidTime());
    }

    /**
     * 根据传入的name值，清洗Redis缓存内数据
     */
    public void flushRedisValue(String name) {
        getMap(name).clear();
    }

    /**
     * 删除Map中的指定键
     *
     * @param name Map名称
     * @param key  要删除的键
     * @return 如果键存在并被删除，返回true；否则返回false
     */
    public boolean deleteMapValue(String name, Object key) {
        RMap<Object, Object> map = getMap(name);
        return map.remove(key) != null;
    }

    /**
     * 批量删除Map中的多个键
     *
     * @param name Map名称
     * @param keys 要删除的键集合
     */
    public void deleteMapValues(String name, Collection<?> keys) {
        RMap<Object, Object> map = getMap(name);
        for (Object key : keys) {
            map.remove(key);
        }
    }
}
