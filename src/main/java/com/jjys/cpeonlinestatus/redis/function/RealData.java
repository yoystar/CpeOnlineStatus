package com.jjys.cpeonlinestatus.redis.function;

@FunctionalInterface
public interface RealData<T> {
    T get();
}
