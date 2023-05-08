package com.kakaoscan.profile.domain.client;

import com.kakaoscan.profile.domain.bridge.BridgeInstance;
import com.kakaoscan.profile.domain.bridge.ClientPayload;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFutureListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

import static java.util.Objects.nonNull;

@Log4j2
@RequiredArgsConstructor
@Component
public class NettyClientInstance {
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Bootstrap bootstrap[] = {new Bootstrap()};

    /**
     * 서버 접속 tick, -1: 연결 X
     */
    private static long startTime = -1;

    public Bootstrap[] getBootstrap() {
        return bootstrap;
    }

    public void setStartTime(long startTime) {
        lock.lock();
        try {
            NettyClientInstance.startTime = startTime;
        } finally {
            lock.unlock();
        }
    }

    public boolean isConnected() {
        return startTime > 0;
    }

    /**
     * 서버 재연결
     */
    public void connect(int index, String session) {
        bootstrap[index].connect().addListener((ChannelFutureListener) future -> {
            if (nonNull(future.cause())) {
                setStartTime(-1);
                log.error("[server connect fail] " + future.cause());

                ClientPayload clientPayload = BridgeInstance.getPayloadBySessionId(session);
                clientPayload.setConnectFail(true);
                BridgeInstance.addPayload(clientPayload);
            }
        });
    }
}
