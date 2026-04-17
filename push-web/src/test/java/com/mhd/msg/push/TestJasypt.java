package com.mhd.msg.push;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.junit.jupiter.api.Test;

/**
 * @author zhao-hao-dong

 */
public class TestJasypt {
    @Test
    void testEncrypt() {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        // 设置加密算法
        encryptor.setAlgorithm("PBEWithMD5AndDES");
        // 设置加密盐
        encryptor.setPassword("QSXRYJNF4510SDCC");
        // 要加密的文本
        String name = encryptor.encrypt("xxxxxxxx");
        // 输出加密后的文本
        System.out.println("name = " + name);
    }
}
