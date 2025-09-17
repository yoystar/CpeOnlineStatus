package com.jjys.cpeonlinestatus.bean.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


/**
 * 设备信息视图对象 t04_device_info
 */
@Data
@AutoMapper(target = DeviceInfoVo.class)
public class DeviceInfoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 自增主键id
     */
    private String id;

    /**
     * 设备所属的模型id（关联t02_model_info表）
     */
    private Long modelId;


    /**
     * 设备产品-所属用户id（关联 t20_device_user_info表）
     */
    private Long userId;

    /**
     * 设备产品-厂商id（关联t01_company_info表）
     */
    private Long companyId;


    /**
     * 产品设备-代理商id（关联t08_agent_info表）
     */
    private Long agentId;

    /**
     * 设备产品-名称
     */
    private String deviceName;

    /**
     * 设备产品-内网/私有IP
     */
    private String privateIp;

    /**
     * 设备产品-公网IP
     */
    private String deviceIp;

    /**
     * 设备产品-公网端口
     */
    private String devicePort;

    /**
     * 设备产品-公网IP-备用
     */
    private String deviceIpBackup;

    /**
     * 设备产品-公网端口-备用
     */
    private String devicePortBackup;

    /**
     * 设备产品-协议版本
     */
    private String specVersion;

    /**
     * 设备产品-ACS访问CPE的地址
     */
    private String deviceUrl;

    /**
     * 设备产品-http路径
     */
    private String deviceHttpPath;

    /**
     * 设备产品-0 前 1.后表示NAT前还是NAT后
     */
    private Long deviceNatDectected;

    /**
     * 设备产品-序列号SN
     */
    private String deviceSn;

    /**
     * 设备产品-序列号ESN
     */
    private String deviceEsn;

    /**
     * 设备产品-描述
     */
    private String deviceDescription;

    /**
     * 设备产品-在线状态id( 关联terminal_status_type字典数据，预留字段，在线状态用redis记录)
     */
    private Long deviceStatus;

    /**
     * 设备产品-宽带连接模式（关联terminal_link_type字典数据）
     */
    private Long deviceLinkType;

    /**
     * 设备产品-音频模式（关联terminal_audio_type字典数据）
     */
    private Long deviceAudioType;

    /**
     * 设备产品-HDMI连接（关联terminal_hdmi_type字典数据）
     */
    private Long deviceHdmiType;

    /**
     * 设备产品-分辨率
     */
    private String resolutionRatio;

    /**
     * 设备产品-锁定状态
     */
    private String deviceLockStatus;

    /**
     * 设备产品-MAC地址
     */
    private String deviceMac;

    /**
     * 设备产品-时区
     */
    private String localTimeZone;

    /**
     * 设备产品-软件版本号
     */
    private String softwareVersion;

    /**
     * 设备产品-附加软件版本号
     */
    private String additionalSoftwareVersion;

    /**
     * 设备产品-硬件版本号
     */
    private String hardwareVersion;

    /**
     * 设备产品-附加硬件版本号
     */
    private String additionalHardwareVersion;

    /**
     * 设备产品-首次登录时间
     */
    private Date firstLoginTime;

    /**
     * 设备产品-最后登录时间
     */
    private Date lastLoginTime;

    /**
     * 设备产品-上次重启时间
     */
    private Date rebootTime;

    /**
     * 设备产品-重启人员
     */
    private String rebootUser;

    /**
     * 设备产品-恢复出厂时间
     */
    private Date resetTime;

    /**
     * 设备产品-恢复出厂设置人员
     */
    private String resetUser;

    /**
     * 最后一次离线时间
     */
    private Date offlineTime;

    /**
     * Launcher应用版本
     */
    private String launcherVersion;
}
