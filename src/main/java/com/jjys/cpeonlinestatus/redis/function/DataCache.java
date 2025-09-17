package com.jjys.cpeonlinestatus.redis.function;

@FunctionalInterface
public interface DataCache<T> {
    Boolean isCache(T paramT);
}