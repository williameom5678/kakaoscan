package com.kakaoscan.profile.domain.bridge;

import lombok.extern.log4j.Log4j2;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TCP Server <-> Netty TCP Client <-> WebSocket Server
 */
@Log4j2
public class BridgeInstance {
    private BridgeInstance() {}

    private static final ReentrantLock lock = new ReentrantLock();
    /**
     * 연결 된 클라이언트 목록
     */
    private static final Map<String, ClientQueue> clients = new ConcurrentHashMap<>();
    /**
     * to tcp
     */
    private static String message = new String();

    public static Map<String, ClientQueue> getClients() {
        return clients;
    }

    /**
     * 세션의 대기 순번을 구한다
     * @param session
     * @return
     */
    public static long getTurn(String session) {
        List<Map.Entry<String, ClientQueue>> clientList = new LinkedList<>(clients.entrySet());
        clientList.sort((Comparator.comparingLong(o -> o.getValue().getRequestTick())));

        for (int i = 0; i < clientList.size(); i++) {
            if (clientList.get(i).getKey().equals(session)) {
                return i;
            }
        }

        return -1;
    }

    public static String getMessage() {
        return message;
    }

    /**
     * 소켓 서버에 메세지를 보낸다
     * @param message
     */
    public static void send(String message) {
        lock.lock();
        try {
            BridgeInstance.message = message;
        } finally {
            lock.unlock();
        }
    }

    public static void clear() {
        send("");
    }
}