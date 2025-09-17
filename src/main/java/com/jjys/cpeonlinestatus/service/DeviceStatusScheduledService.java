package com.jjys.cpeonlinestatus.service;

import com.jjys.cpeonlinestatus.bean.DeviceInfo;
import com.jjys.cpeonlinestatus.bean.bo.DeviceInfoBo;
import com.jjys.cpeonlinestatus.constant.AutoRegisterConstant;
import com.jjys.cpeonlinestatus.constant.RedisConstant;
import com.jjys.cpeonlinestatus.constant.RedisExpireConstant;
import com.jjys.cpeonlinestatus.enums.DeviceStatus;
import com.jjys.cpeonlinestatus.redis.redisson.RedissonCollectionCache;
import com.jjys.cpeonlinestatus.redis.utils.RedisUtils;
import com.jjys.cpeonlinestatus.utils.MapstructUtils;
import com.jjys.cpeonlinestatus.utils.SpringUtils;
import com.jjys.cpeonlinestatus.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBitSet;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 保存、获取、管理设备在线状态 调度服务类
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class DeviceStatusScheduledService implements InitializingBean {
    private static IDeviceInfoService deviceInfoService;
    private static RedissonCollectionCache redissonCollectionCache;

    //调度任务时间时间偏移量，防止调度时间与锁时间冲突，单位：秒
    private static Integer SCHEDULE_TIME_OFFSET = 1;
    //CPE配置的ID
    private static String CPE_CONFIG_ID;
    //滑动时间窗口间隔，单位：秒
    private static Integer SLIDING_TIME;
    //设备心跳时间间隔，单位：秒
    private static Integer CPE_HEARTBEAT;

    private static final String DEVICE_STATUS = "device_status:";
    private static final String BITSET_KEY_PREFIX = DEVICE_STATUS + "bitset_";
    private static final String BITSET_INDEX = DEVICE_STATUS + "bitset_index";
    private static final String BITSET_CHANGE_LOCK = DEVICE_STATUS + "bitset_change_lock";
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Value("${cpe.config.id}")
    public void setCpeConfigId(String cpeConfigId) {
        CPE_CONFIG_ID = cpeConfigId;
    }

    @Value("${device_status.sliding_time}")
    public void setSlidingTime(Integer slidingTime) {
        SLIDING_TIME = slidingTime;
    }

    @Override
    public void afterPropertiesSet() {
        //TODO: 需要修改成自定义的心跳间隔
        CPE_HEARTBEAT = 1800;
        log.info("--->CPE心跳间隔为:{}", CPE_HEARTBEAT);

        //定时调度任务，用于滑动时间窗口
        scheduler.scheduleAtFixedRate(DeviceStatusScheduledService::expireAndCreateBitsetWithLock,
                SLIDING_TIME + SCHEDULE_TIME_OFFSET,
                SLIDING_TIME + SCHEDULE_TIME_OFFSET,
                TimeUnit.SECONDS);
        log.info("--->滑动时间窗口间隔为:{}", SLIDING_TIME);
    }

    /**
     * 过期删除正在使用的bitset 并滚动创建新的bitset
     * 使用Redis分布式锁来确保只有一个微服务节点执行expireAndCreateBitsetWithLock任务
     */
    private static void expireAndCreateBitsetWithLock() {
        try {
            boolean lockAcquired = getBitsetChangeLock();
//            log.info("--->过期删除正在使用的bitset 并滚动创建新的bitset 分布式lock = " + lockAcquired);
            if (lockAcquired) {
                int currentIndex = getBitsetIndex();
                String bitsetKeyToExpire = BITSET_KEY_PREFIX + currentIndex;
                slideBitsetIndex();
                //滑动bitset的index之后 再过期处理当前使用的bitset
                deleteBitset(bitsetKeyToExpire);
                //同步在线状态到Sql中
                syncDeviceStatusToSql();
//                log.info("--->过期Bitset的key键名称: {}", bitsetKeyToExpire);
            }
        } catch (Exception e) {
            log.warn("--->过期删除正在使用的bitset 并滚动创建新的bitset 异常:", e);
        }
    }

    /**
     * 读取切换bitset的锁标识
     * 如果 不存在 则设置 并返回 true 如果 存在 则不做任何操作 返回 false
     */
    public static boolean getBitsetChangeLock() {
        return RedisUtils.setObjectIfAbsent(BITSET_CHANGE_LOCK, "Bitset_Change_Locked", Duration.ofSeconds(SLIDING_TIME));
    }

    /**
     * 根据设备心跳间隔+滑动时间窗口 计算需要的bitset数量（向下取整数）,最小值为 1
     */
    public static int countBitsetNum() {
        return Math.max((CPE_HEARTBEAT / SLIDING_TIME), 1);
    }

    /**
     * 设置设备在线状态
     *
     * @param deviceIdStr 设备ID
     * @param status      在线状态
     */
    public static void setDeviceStatus(String deviceIdStr, Long status) {
        if (StringUtils.isBlank(deviceIdStr) || null == status) return;
        long deviceId = Long.parseLong(deviceIdStr);
        int start = getBitsetIndex();
        int end = getBitsetIndex() + countBitsetNum();
        for (int i = start; i < end; i++) {
            RedisUtils.setCacheBitSet(BITSET_KEY_PREFIX + i, deviceId, status == DeviceStatus.ONLINE.getCode(),
                    RedisExpireConstant.deviceStatusBitsetExpirationTime);
        }
    }

    /**
     * 获取设备在线状态
     *
     * @param deviceIdStr 设备ID字符串
     * @return 在线状态 (ONLINE 表示在线, OFFLINE 表示离线)
     */
    public static Long getDeviceStatus(String deviceIdStr) {
        long deviceId = Long.parseLong(deviceIdStr);
        int index = getBitsetIndex();
        return RedisUtils.getCacheBitSet(BITSET_KEY_PREFIX + index, deviceId) ? DeviceStatus.ONLINE.getCode() : DeviceStatus.OFFLINE.getCode();
    }


    /**
     * 根据设备sn，获取设备在线状态
     *
     * @param deviceSnStr 设备ID字符串
     * @return 在线状态 (ONLINE 表示在线, OFFLINE 表示离线)
     */
    public static Long getDeviceStatusBySn(String deviceSnStr) {
        DeviceInfo deviceInfo = getRedissonCollectionCache().getMapCacheValue(RedisConstant.Biz.baseDeviceInfoMap,
                deviceSnStr, () -> {
                    return (MapstructUtils.convert(getDeviceInfoService().queryBaseBySn(deviceSnStr), DeviceInfo.class));
                }, RedisExpireConstant.deviceInfoMapExpirationTime);
        if (null != deviceInfo) {
            return getDeviceStatus(deviceInfo.getId());
        } else {
            return DeviceStatus.OFFLINE.getCode();
        }
    }

    /**
     * 查询在线设备总数
     */
    public static Long getOnlineDeviceCount() {
        int index = getBitsetIndex();
        return RedisUtils.countCacheBitSet(BITSET_KEY_PREFIX + index);
    }

    /**
     * 根据key清空Bitset内的信息
     *
     * @param key Bitmap键
     */
    public static void clearBitset(String key) {
        RedisUtils.clearCacheBitSet(key);
    }

    /**
     * 根据key删除Bitset对象
     *
     * @param key Bitmap键
     */
    public static boolean deleteBitset(String key) {
        return RedisUtils.deleteObject(key);
    }

    /**
     * 清空所有Bitset+Lock+Index信息
     */
    public static boolean deleteAllDeviceStatusCache() {
        boolean allDeleted = false;
        int start = getBitsetIndex();
        int end = getBitsetIndex() + countBitsetNum();
        for (int i = start; i <= end; i++) {
            boolean bitsetDeleted = RedisUtils.deleteObject(BITSET_KEY_PREFIX + i);
            allDeleted = allDeleted && bitsetDeleted;
        }
        boolean lockDeleted = RedisUtils.deleteObject(BITSET_CHANGE_LOCK);
        boolean indexDeleted = RedisUtils.deleteObject(BITSET_INDEX);
        return allDeleted && lockDeleted && indexDeleted;
    }


    /**
     * 获取bitset当前的index
     * 如果不存在则返回 默认值：1
     * 如果存在则返回 当前值
     */
    public static Integer getBitsetIndex() {
        int index = 1;
        Integer bitsetIndex = RedisUtils.getCacheObject(BITSET_INDEX);
        if (bitsetIndex != null && bitsetIndex > 0) {
            index = bitsetIndex;
        }
        return index;
    }

    /**
     * 设置bitset当前的index
     */
    public static void setBitsetIndex(int index) {
        RedisUtils.setCacheObject(BITSET_INDEX, index);
    }

    /**
     * 滑动增加bitset的index
     * 如果不存在则设置为 默认值：1
     * 如果存在并且值大于0则设置为 当前值+1
     */
    public static void slideBitsetIndex() {
        int index = 1;
        Integer bitsetIndex = getBitsetIndex();
        if (bitsetIndex != null && bitsetIndex > 0) {
            index = bitsetIndex;
        }
        setBitsetIndex(index + 1);
//        log.info("--->滑动Bitset的Index索引到: {}", index + 1);
    }

    /**
     * 定时将已下线的设备信息 同步到mysql中
     * 查询mysql中2个心跳周期内上过线、且当前状态为online的设备， 与redis数据对比 如果已离线则更新mysql数据
     */
    public static void syncDeviceStatusToSql() {
//        log.info("--->定时将已下线的设备信息 同步到mysql中");
        List<DeviceInfo> deviceInfos = new ArrayList<>();
        RBitSet rBitSet = RedisUtils.getBitsetObject(BITSET_KEY_PREFIX + getBitsetIndex());
        //获取2个心跳周期前的时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.SECOND, -(CPE_HEARTBEAT * 2));
        Date resultDate = calendar.getTime();

        //查询2个心跳周期内、当前状态是online设备
        DeviceInfoBo deviceInfoBo = new DeviceInfoBo();
        deviceInfoBo.setDeviceStatus(DeviceStatus.ONLINE.getCode());
        deviceInfoBo.setLastLoginTime(resultDate);
        getDeviceInfoService().queryDeviceStatusList(deviceInfoBo).forEach(deviceInfo -> {
            //判断Bitset中如果设备在线状态为离线 -> 则更新数据库状态为离线
            if (!rBitSet.get(Long.parseLong(deviceInfo.getId()))) {
                deviceInfo.setDeviceStatus(DeviceStatus.OFFLINE.getCode());
                deviceInfo.setOfflineTime(new Date());
                deviceInfo.setUpdateBy(AutoRegisterConstant.UPDATE_BY_AUTO_REGISTER);
                deviceInfos.add(deviceInfo);
            }
        });
        //符合条件的设备在线状态 批量修改成offline
        if (deviceInfos != null && deviceInfos.size() > 0) {
            getDeviceInfoService().updateDeviceStatusBatch(deviceInfos);
        }
    }

    /**
     * 将已下线的全部设备信息 同步到mysql中
     * 查询当前状态为online的设备， 与redis数据对比 如果已离线则更新mysql数据
     */
    public static void syncAllDeviceStatusToSql() {
        log.info("--->将已下线的所有设备信息 同步到mysql中");
        List<DeviceInfo> deviceInfos = new ArrayList<>();
        RBitSet rBitSet = RedisUtils.getBitsetObject(BITSET_KEY_PREFIX + getBitsetIndex());

        //查询当前状态是online设备
        DeviceInfoBo deviceInfoBo = new DeviceInfoBo();
        deviceInfoBo.setDeviceStatus(DeviceStatus.ONLINE.getCode());
        getDeviceInfoService().queryDeviceStatusList(deviceInfoBo).forEach(deviceInfo -> {
            //判断Bitset中如果设备在线状态为离线 -> 则更新数据库状态为离线
            if (!rBitSet.get(Long.parseLong(deviceInfo.getId()))) {
                deviceInfo.setDeviceStatus(DeviceStatus.OFFLINE.getCode());
                deviceInfo.setOfflineTime(new Date());
                deviceInfo.setUpdateBy(AutoRegisterConstant.UPDATE_BY_AUTO_REGISTER);
                deviceInfos.add(deviceInfo);
            }
        });
        //符合条件的设备在线状态 批量修改成offline
        if (deviceInfos != null && deviceInfos.size() > 0) {
            getDeviceInfoService().updateDeviceStatusBatch(deviceInfos);
        }
    }

    /**
     * 给静态的deviceInfoService赋值
     */
    public static IDeviceInfoService getDeviceInfoService() {
        if (null == deviceInfoService)
            deviceInfoService = SpringUtils.getApplicationContext().getBean(IDeviceInfoService.class);
        return deviceInfoService;
    }

    /**
     * 给静态的redissonCollectionCache赋值
     */
    public static RedissonCollectionCache getRedissonCollectionCache() {
        if (null == redissonCollectionCache)
            redissonCollectionCache = SpringUtils.getApplicationContext().getBean(RedissonCollectionCache.class);
        return redissonCollectionCache;
    }

}
