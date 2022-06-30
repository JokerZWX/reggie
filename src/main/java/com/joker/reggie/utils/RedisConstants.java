package com.joker.reggie.utils;

public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long CACHE_CODE_TTL = 5L;
    public static final String LOGIN_USER_KEY = "login:user:";
    public static final Long CACHE_USER_TTL = 30L;

    public static final Long CACHE_DISHDTO_TTL = 60L;

    public static final String USER_NICK_NAME_PREFIX = "user_";
    public static final String DISH_NICK_NAME_PREFIX = "dish_";
}
