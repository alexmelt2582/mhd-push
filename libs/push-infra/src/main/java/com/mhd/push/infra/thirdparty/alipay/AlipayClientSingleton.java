package com.mhd.push.infra.thirdparty.alipay;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.mhd.push.common.constant.SendChanelUrlConstant;
import com.mhd.push.domain.model.account.AlipayMiniProgramAccount;

import java.util.HashMap;
import java.util.Map;

/**
 * 初始化支付宝小程序 单例
 */
public class AlipayClientSingleton {


    private static final Map<String, DefaultAlipayClient> ALIPAY_CLIENT_MAP = new HashMap<>();

    private AlipayClientSingleton() {
    }

    public static DefaultAlipayClient getSingleton(AlipayMiniProgramAccount alipayMiniProgramAccount) throws AlipayApiException {
        if (!ALIPAY_CLIENT_MAP.containsKey(alipayMiniProgramAccount.getAppId())) {
            synchronized (DefaultAlipayClient.class) {
                if (!ALIPAY_CLIENT_MAP.containsKey(alipayMiniProgramAccount.getAppId())) {
                    AlipayConfig alipayConfig = new AlipayConfig();
                    alipayConfig.setServerUrl(SendChanelUrlConstant.ALI_MINI_PROGRAM_GATEWAY_URL);
                    alipayConfig.setAppId(alipayMiniProgramAccount.getAppId());
                    alipayConfig.setPrivateKey(alipayMiniProgramAccount.getPrivateKey());
                    alipayConfig.setFormat("json");
                    alipayConfig.setAlipayPublicKey(alipayMiniProgramAccount.getAlipayPublicKey());
                    alipayConfig.setCharset("utf-8");
                    alipayConfig.setSignType("RSA2");
                    ALIPAY_CLIENT_MAP.put(alipayMiniProgramAccount.getAppId(), new DefaultAlipayClient(alipayConfig));
                }
            }
        }
        return ALIPAY_CLIENT_MAP.get(alipayMiniProgramAccount.getAppId());
    }
}
