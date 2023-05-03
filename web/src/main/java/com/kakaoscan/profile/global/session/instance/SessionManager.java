package com.kakaoscan.profile.global.session.instance;

public interface SessionManager {
    public static final String SESSION_FORMAT = "user::%s";
    void add(String key, Object value);
    Object get(String key);
    void delete(String key);
}
