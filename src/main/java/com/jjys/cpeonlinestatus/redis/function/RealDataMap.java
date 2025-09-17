package com.jjys.cpeonlinestatus.redis.function;

import java.util.Map;

@FunctionalInterface
public interface RealDataMap<K, V> {
    Map<K, V> get();
}
