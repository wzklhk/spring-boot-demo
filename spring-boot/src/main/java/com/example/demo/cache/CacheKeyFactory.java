package com.example.demo.cache;

/** 缓存 key 工厂：Redis 物理 key 由 cacheName + key 组装（如 sb:user:3、sb:user:username:admin） */
public final class CacheKeyFactory {

    private CacheKeyFactory() {
    }

    public static String id(Object id) {
        return String.valueOf(id);
    }

    /** 用户名索引 key（用于按用户名缓存/失效用户资料） */
    public static String username(String username) {
        return "username:" + username;
    }
}