package com.kakaoscan.profile.domain.bridge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@Builder
public class ClientPayload implements Comparable<ClientPayload> {
    private String session;
    /**
     * 요청 시간순으로 정렬
     */
    private long requestTick;
    /**
     * 마지막 수신 tick
     */
    private long lastReceivedTick;
    /**
     * 마지막 send tick
     */
    private long lastSendTick;
    /**
     * 요청 전화번호
     */
    private String request;
    /**
     * view 응답
     */
    private String response;
    /**
     * connect flag
     */
    private boolean tryConnect;
    /**
     * response connected state
     */
    private boolean connectFail;

    private int priority;

    @Override
    public int compareTo(ClientPayload o) {
        int comp = Long.compare(this.requestTick, o.requestTick);
        if (comp != 0) {
            return comp;
        }
        return this.session.compareTo(o.session);
    }
}
