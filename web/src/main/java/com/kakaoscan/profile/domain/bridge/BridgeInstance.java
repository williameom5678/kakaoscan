package com.kakaoscan.profile.domain.bridge;

import lombok.extern.log4j.Log4j2;

import java.util.concurrent.ConcurrentSkipListSet;
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
    private static final ConcurrentSkipListSet<ClientPayload> clients = new ConcurrentSkipListSet<>();
    /**
     * to tcp
     */
    private static String message = new String();

    public static void addPayload(ClientPayload clientPayload) {
        removePayloadBySessionId(clientPayload.getSession());

        clients.add(clientPayload);
        int priority = 0;
        for (ClientPayload payload : clients) {
            payload.setPriority(priority++);
        }
    }

    public static ClientPayload getPayloadBySessionId(String session) {
        for (ClientPayload payload : clients) {
            if (session.equals(payload.getSession())) {
                return payload;
            }
        }
        throw new NullPointerException();
    }

    public static void removePayloadBySessionId(String session) {
        ClientPayload toRemove = null;
        for (ClientPayload payload : clients) {
            if (session.equals(payload.getSession())) {
                toRemove = payload;
                break;
            }
        }
        if (toRemove != null) {
            clients.remove(toRemove);
        }
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