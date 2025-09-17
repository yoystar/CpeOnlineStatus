package com.jjys.cpeonlinestatus.redis.function;

import java.util.Set;

@FunctionalInterface
public interface RealDataSet<T> {
    Set<T> get();
}

