package com.jjys.cpeonlinestatus.redis.function;

import java.util.List;

@FunctionalInterface
public interface RealDataList<T> {
    List<T> get();
}
