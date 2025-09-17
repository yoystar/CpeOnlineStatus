package com.jjys.cpeonlinestatus.mapper;

import com.jjys.cpeonlinestatus.bean.DeviceInfo;
import com.jjys.cpeonlinestatus.bean.bo.DeviceInfoBo;
import com.jjys.cpeonlinestatus.bean.vo.DeviceInfoVo;

import java.util.List;

/**
 * 设备信息Mapper接口
 */
public interface DeviceInfoMapper extends BaseMapperPlus<DeviceInfo, DeviceInfoVo> {
    /**
     * 根据sn 查询设备<基本>信息
     *
     * @param sn
     */
    DeviceInfoVo queryBaseBySn(String sn);

    /**
     * 以最后上线时间为条件，查询在线设备(id+在线状态+最后上线时间信息)列表
     */
    List<DeviceInfo> queryDeviceStatusList(DeviceInfoBo bo);

}
