package com.kakaoscan.profile.utils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Optional;

public class HttpRequestUtils {
    public static String getRemoteAddress(HttpServletRequest request) {
        String remoteAddress = (null != request.getHeader("X-FORWARDED-FOR")) ? request.getHeader("X-FORWARDED-FOR") : request.getRemoteAddr();
        String ra = remoteAddress.replace("0:0:0:0:0:0:0:1", "127.0.0.1");
        if (ra.contains(",")) {
            ra = ra.split(",")[0];
        }
        try {
            InetAddress inetAddress = InetAddress.getByName(ra);
            if (inetAddress instanceof Inet4Address) {
                return ra;
            } else {
                byte[] bytes = inetAddress.getAddress();
                InetAddress v4Address = Inet4Address.getByAddress(bytes);
                return v4Address.getHostAddress();
            }
        } catch (UnknownHostException e) {
            return ra;
        }
    }

    @Deprecated
    public static Optional<Cookie> getCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> "email-hash".equals(cookie.getName()))
                .findFirst();
    }
}
