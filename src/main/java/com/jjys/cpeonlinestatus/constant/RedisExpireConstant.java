package com.jjys.cpeonlinestatus.constant;


/**
 * Redis过期时间常量
 * Redis过期时间单位:ms
 */

public class RedisExpireConstant {
    public static Long deviceStatusBitsetExpirationTime = Long.valueOf(86400L);
    public static Long deviceInfoMapExpirationTime = Long.valueOf(1800000L);
}