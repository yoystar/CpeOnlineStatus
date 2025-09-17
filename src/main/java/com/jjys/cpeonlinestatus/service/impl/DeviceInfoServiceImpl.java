package com.jjys.cpeonlinestatus.service.impl;

import com.jjys.cpeonlinestatus.bean.DeviceInfo;
import com.jjys.cpeonlinestatus.bean.bo.DeviceInfoBo;
import com.jjys.cpeonlinestatus.bean.vo.DeviceInfoVo;
import com.jjys.cpeonlinestatus.enums.DeviceStatus;
import com.jjys.cpeonlinestatus.mapper.DeviceInfoMapper;
import com.jjys.cpeonlinestatus.service.DeviceStatusScheduledService;
import com.jjys.cpeonlinestatus.service.IDeviceInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 设备信息Service业务层处理
 */
@RequiredArgsConstructor
@Service
public class DeviceInfoServiceImpl implements IDeviceInfoService {

    private final DeviceInfoMapper baseMapper;

    /**
     * 根据sn 查询设备<基本>信息
     *
     * @param sn
     */
    @Override
    public DeviceInfoVo queryBaseBySn(String sn) {
        DeviceInfoVo deviceInfoVo = baseMapper.queryBaseBySn(sn);
        //从redis中获取终端设备在线状态(排除未启用设备)
        if (null != deviceInfoVo && deviceInfoVo.getDeviceStatus() != DeviceStatus.NOT_ENABLED.getCode())
            deviceInfoVo.setDeviceStatus(DeviceStatusScheduledService.getDeviceStatus(deviceInfoVo.getId()));
        return deviceInfoVo;
    }

    /**
     * 以最后上线时间为条件，查询在线设备(id+在线状态+最后上线时间信息)列表
     *
     * @param bo
     */
    @Override
    public List<DeviceInfo> queryDeviceStatusList(DeviceInfoBo bo) {
        return baseMapper.queryDeviceStatusList(bo);
    }

    /**
     * 批量修改设备在线状态
     *
     * @param deviceInfos
     */
    @Override
    public Boolean updateDeviceStatusBatch(List<DeviceInfo> deviceInfos) {
        boolean flag = baseMapper.updateBatchById(deviceInfos);
        return flag;
    }

}
