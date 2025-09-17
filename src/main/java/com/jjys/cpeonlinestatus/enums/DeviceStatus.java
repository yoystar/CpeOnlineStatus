package com.jjys.cpeonlinestatus.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 设备在线状态
 *
 */
@Getter
@AllArgsConstructor
public enum DeviceStatus {
    /**
     * 离线
     */
    OFFLINE(0L, "离线"),
    /**
     * 在线
     */
    ONLINE(1L, "在线"),
    /**
     * 未启用
     */
    NOT_ENABLED(2L, "未启用");


    private final long code;
    private final String info;

}
