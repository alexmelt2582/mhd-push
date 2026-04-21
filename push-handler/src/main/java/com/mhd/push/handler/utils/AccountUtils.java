package com.mhd.push.handler.utils;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaRedisBetterConfigImpl;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.base.Throwables;
import com.mhd.push.common.dto.account.WeChatMiniProgramAccount;
import com.mhd.push.common.dto.account.WeChatOfficialAccount;
import com.mhd.push.common.dto.account.sms.SmsAccount;
import com.mhd.push.common.enums.ChannelType;
import com.mhd.push.support.domain.entity.ChannelAccount;
import com.mhd.push.support.mapper.ChannelAccountMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.redis.RedisTemplateWxRedisOps;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.config.impl.WxMpRedisConfigImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 获取账号信息工具类
 *
 * @author zhao-hao-dong
 */
@Slf4j
@Configuration
public class AccountUtils {

    @Resource
    private ChannelAccountMapper channelAccountMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 消息的小程序/微信服务号账号
     */
    private final ConcurrentMap<ChannelAccount, WxMpService> officialAccountServiceMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<ChannelAccount, WxMaService> miniProgramServiceMap = new ConcurrentHashMap<>();

    @Bean
    public RedisTemplateWxRedisOps redisTemplateWxRedisOps() {
        return new RedisTemplateWxRedisOps(this.stringRedisTemplate);
    }

    /**
     * 微信小程序：返回 WxMaService
     * 微信服务号：返回 WxMpService
     * 其他渠道：返回XXXAccount账号对象
     *
     * @param sendAccountId
     * @param clazz
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getAccountById(Integer sendAccountId, Class<T> clazz) {
        try {
            ChannelAccount dbchannelAccount = getChannelAccountById(sendAccountId);
            if (Objects.nonNull(dbchannelAccount)) {
                if (clazz.equals(WxMaService.class)) {
                    return (T) ConcurrentHashMapUtils.computeIfAbsent(miniProgramServiceMap, dbchannelAccount, account -> initMiniProgramService(JSON.parseObject(account.getAccountConfig(), WeChatMiniProgramAccount.class)));
                } else if (clazz.equals(WxMpService.class)) {
                    return (T) ConcurrentHashMapUtils.computeIfAbsent(officialAccountServiceMap, dbchannelAccount, account -> initOfficialAccountService(JSON.parseObject(account.getAccountConfig(), WeChatOfficialAccount.class)));
                } else {
                    return JSON.parseObject(dbchannelAccount.getAccountConfig(), clazz);
                }
            }
        } catch (Exception e) {
            log.error("AccountUtils#getAccount fail! e:{}", Throwables.getStackTraceAsString(e));
        }
        return null;
    }

    /**
     * 读取原始渠道账号实体。
     *
     * 限流、运维查询等场景需要拿到账户原始配置，此方法避免到处直接访问 Mapper。
     */
    public ChannelAccount getChannelAccountById(Integer sendAccountId) {
        if (sendAccountId == null) {
            return null;
        }
        try {
            return channelAccountMapper.selectById(Long.valueOf(sendAccountId));
        } catch (Exception e) {
            log.error("AccountUtils#getChannelAccountById fail! e:{}", Throwables.getStackTraceAsString(e));
            return null;
        }
    }

    /**
     * 通过脚本名 匹配到对应的短信账号
     *
     * @param scriptName 脚本名
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> T getSmsAccountByScriptName(String scriptName, Class<T> clazz) {
        try {
            LambdaQueryWrapper<ChannelAccount> queryWrapper = Wrappers.lambdaQuery();
            queryWrapper
                    .eq(ChannelAccount::getSendChannel, ChannelType.SMS.getCode());
            List<ChannelAccount> channelAccountList = channelAccountMapper.selectList(queryWrapper);
            for (ChannelAccount channelAccount : channelAccountList) {
                try {
                    SmsAccount smsAccount = JSON.parseObject(channelAccount.getAccountConfig(), SmsAccount.class);
                    if (smsAccount.getScriptName().equals(scriptName)) {
                        return JSON.parseObject(channelAccount.getAccountConfig(), clazz);
                    }
                } catch (Exception e) {
                    log.error("AccountUtils#getSmsAccount parse fail! e:{},account:{}", Throwables.getStackTraceAsString(e), JSON.toJSONString(channelAccount));
                }
            }
        } catch (Exception e) {
            log.error("AccountUtils#getSmsAccount fail! e:{}", Throwables.getStackTraceAsString(e));
        }
        log.error("AccountUtils#getSmsAccount not found!:{}", scriptName);
        return null;
    }

    /**
     * 初始化微信服务号
     * access_token 用redis存储
     *
     * @return
     */
    public WxMpService initOfficialAccountService(WeChatOfficialAccount officialAccount) {
        WxMpService wxMpService = new WxMpServiceImpl();
        WxMpRedisConfigImpl config = new WxMpRedisConfigImpl(redisTemplateWxRedisOps(), ChannelType.OFFICIAL_ACCOUNT.getAccessTokenPrefix());
        config.setAppId(officialAccount.getAppId());
        config.setSecret(officialAccount.getSecret());
        config.setToken(officialAccount.getToken());
        config.useStableAccessToken(true);
        wxMpService.setWxMpConfigStorage(config);
        return wxMpService;
    }

    /**
     * 初始化微信小程序
     * access_token 用redis存储
     *
     * @return
     */
    private WxMaService initMiniProgramService(WeChatMiniProgramAccount miniProgramAccount) {
        WxMaService wxMaService = new WxMaServiceImpl();
        WxMaRedisBetterConfigImpl config = new WxMaRedisBetterConfigImpl(redisTemplateWxRedisOps(), ChannelType.MINI_PROGRAM.getAccessTokenPrefix());
        config.setAppid(miniProgramAccount.getAppId());
        config.setSecret(miniProgramAccount.getAppSecret());
        config.useStableAccessToken(true);
        wxMaService.setWxMaConfig(config);
        return wxMaService;
    }


}
