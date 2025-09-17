package com.jjys.cpeonlinestatus.service;

import com.jjys.cpeonlinestatus.bean.DeviceInfo;
import com.jjys.cpeonlinestatus.bean.bo.DeviceInfoBo;
import com.jjys.cpeonlinestatus.bean.vo.DeviceInfoVo;

import java.util.List;

/**
 * 设备信息Service接口
 */
public interface IDeviceInfoService {
    /**
     * 根据sn 查询设备<基本>信息
     */
    DeviceInfoVo queryBaseBySn(String sn);

    /**
     * 以最后上线时间为条件，查询在线设备(id+在线状态+最后上线时间信息)列表
     */
    List<DeviceInfo> queryDeviceStatusList(DeviceInfoBo bo);

    /**
     * 批量修改设备在线状态
     */
    Boolean updateDeviceStatusBatch(List<DeviceInfo> deviceInfos);

}
