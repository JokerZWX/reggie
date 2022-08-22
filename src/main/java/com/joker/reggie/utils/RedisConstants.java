package com.joker.reggie.utils;

public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final String LOGIN_USER_KEY = "login:user:";
    public static final String LOGIN_TOKEN_KEY = "login:token:";


    public static final Long LOGIN_TOKEN_TTL = 30L;
    public static final Long CACHE_USER_TTL = 30L;
    public static final Long CACHE_CODE_TTL = 5L;
    public static final Long CACHE_DISHDTO_TTL = 60L;
    public static final Long CACHE_SETMEAL_TTL = 60L;

    public static final String USER_NICK_NAME_PREFIX = "user_";
    public static final String DISH_NAME_PREFIX = "dish_";
    public static final String SETMEAL_NAME_PREFIX = "setmeal_";
}
