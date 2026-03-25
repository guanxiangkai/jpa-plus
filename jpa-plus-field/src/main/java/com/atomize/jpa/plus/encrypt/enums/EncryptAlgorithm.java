package com.atomize.jpa.plus.encrypt.enums;

/**
 * 内置加密算法枚举
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期二
 */
public enum EncryptAlgorithm implements Algorithm {

    AES("AES", "AES"),
    AES_CBC("AES", "AES/CBC/PKCS5Padding"),
    DES("DES", "DES"),
    DES_EDE("DESede", "DESede");

    // ─── 注解用字符串常量 ───

    public static final String AES_NAME = "AES";
    public static final String AES_CBC_NAME = "AES_CBC";

    private final String algorithmName;
    private final String transformation;

    EncryptAlgorithm(String algorithmName, String transformation) {
        this.algorithmName = algorithmName;
        this.transformation = transformation;
    }

    @Override
    public String algorithmName() {
        return algorithmName;
    }

    @Override
    public String transformation() {
        return transformation;
    }
}

