package com.kakaoscan.profile.domain.socket.server;

import com.kakaoscan.profile.domain.socket.common.BridgeInstance;
import com.kakaoscan.profile.domain.socket.common.ClientPayload;
import com.kakaoscan.profile.domain.socket.client.NettyClientInstance;
import com.kakaoscan.profile.domain.dto.UserDTO;
import com.kakaoscan.profile.domain.enums.KafkaEventType;
import com.kakaoscan.profile.domain.enums.MessageSendType;
import com.kakaoscan.profile.domain.enums.Role;
import com.kakaoscan.profile.domain.exception.InvalidAccessException;
import com.kakaoscan.profile.domain.kafka.service.KafkaProducerService;
import com.kakaoscan.profile.domain.model.UseCount;
import com.kakaoscan.profile.domain.service.AccessLimitService;
import com.kakaoscan.profile.domain.service.AddedNumberService;
import com.kakaoscan.profile.domain.service.CacheService;
import com.kakaoscan.profile.domain.service.UserRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static com.kakaoscan.profile.utils.StrUtils.isNumeric;

@Log4j2
@Component
@RequiredArgsConstructor
public class WebSocketServerHandler extends TextWebSocketHandler {
    @Value("${kakaoscan.all.date.maxcount}")
    private int allLimitCount;

    @Value("${kakaoscan.server.count}")
    private long serverCount;

    @Value("${kakaoscan.user.date.maxcount}")
    private int userLimitCount;

    private static final int EVG_WAITING_SEC = 20;
    private static final int REQUEST_TIMEOUT_TICK = 5 * 1000;

    private final NettyClientInstance nettyClientInstance;
    private final AccessLimitService accessLimitService;
    private final UserRequestService userRequestService;
    private final AddedNumberService addedNumberService;
    private final KafkaProducerService producerService;
    private final CacheService cacheService;

    public String getRemoteAddress(WebSocketSession session) {
        Map<String, Object> map = session.getAttributes();
        return (String) map.get("remoteAddress");
    }

    public UserDTO getUser(WebSocketSession session) {
        Map<String, Object> map = session.getAttributes();
        Object userObj = map.get("user");

        if (userObj instanceof UserDTO) {
            return (UserDTO) userObj;
        } else {
            return null;
        }
    }

    private boolean isPhoneNumber(String receive) {
        return isNumeric(receive) && receive.length() == 11;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {

            String receive = message.getPayload();
            if (receive.length() == 0) {
                throw new IllegalArgumentException("empty receive message");
            }

            UserDTO user = getUser(session);
            if (user == null) {
                throw new InvalidAccessException(MessageSendType.USER_NOT_FOUND.getMessage());
            }

            if (user.getRole() == null || Role.GUEST.equals(user.getRole())) {
                throw new InvalidAccessException(MessageSendType.USER_NO_PERMISSION.getMessage());
            }

            ClientPayload clientPayload = BridgeInstance.getPayloadBySessionId(session.getId());

            if (clientPayload.getLastSendTick() != 0 && System.currentTimeMillis() > clientPayload.getLastSendTick()) {
                throw new InvalidAccessException(MessageSendType.REQUEST_TIME_OUT.getMessage());
            }

            // receive phone number
            if (isPhoneNumber(receive) && clientPayload.getRequestTick() == Long.MAX_VALUE) {

                if (!addedNumberService.isExistsPhoneNumberHash(receive)) {
                    // remoteAddress 같은 계정 사용 횟수 동기화
                    userRequestService.syncUserUseCount(getRemoteAddress(session), LocalDate.now());

                    // 클라이언트 일일 사용 제한
                    if (userRequestService.getTodayUseCount(user.getEmail()) >= userLimitCount) {
                        throw new InvalidAccessException(String.format(MessageSendType.LOCAL_ACCESS_LIMIT.getMessage(), userLimitCount));
                    }

                    // 전체 일일 사용 제한
                    UseCount useCount = accessLimitService.getUseCount();
                    if (useCount.getTotalCount() >= allLimitCount * serverCount) {
                        throw new InvalidAccessException(MessageSendType.ACCESS_LIMIT.getMessage());
                    }
                }

                if (!cacheService.isEnabledPhoneNumber(receive)) {
                    throw new InvalidAccessException(MessageSendType.INVALID_NUMBER.getMessage());
                }

                // put turn
                BridgeInstance.addPayload(ClientPayload.builder()
                            .session(session.getId())
                            .requestTick(System.currentTimeMillis())
                            .lastSendTick(System.currentTimeMillis() + REQUEST_TIMEOUT_TICK)
                            .request(receive)
                            .build());
            } else if (MessageSendType.HEARTBEAT.getMessage().equals(receive)) {
                // update
                clientPayload.setLastSendTick(System.currentTimeMillis() + REQUEST_TIMEOUT_TICK);
                BridgeInstance.addPayload(clientPayload);

                int priority = clientPayload.getPriority();
                // 대기큐가 1명 이상이면 남은 대기 시간 전달
                String viewMessage = String.format(MessageSendType.REMAINING_QUEUE.getMessage(), priority, priority * EVG_WAITING_SEC);

                if (priority == 0) {
                    if (!clientPayload.isTryConnect()) {
                        clientPayload.setTryConnect(true);
                        nettyClientInstance.connect(0, session.getId());
                    }

                    // check connected
                    clientPayload = BridgeInstance.getPayloadBySessionId(session.getId());
                    if (clientPayload.isConnectFail()) {
                        throw new InvalidAccessException(MessageSendType.SERVER_INSTANCE_NOT_RUN.getMessage());
                    }

                    // time out
                    if (clientPayload.getLastReceivedTick() != 0 && System.currentTimeMillis() > clientPayload.getLastReceivedTick()) {
                        throw new InvalidAccessException(MessageSendType.REQUEST_TIME_OUT.getMessage());

                    } else if (clientPayload.getLastReceivedTick() == 0) {
                        clientPayload.setLastReceivedTick(System.currentTimeMillis() + REQUEST_TIMEOUT_TICK);
                        BridgeInstance.addPayload(clientPayload);
                    }

                    // send (세션/메세지타입/번호/아이피해시/이메일)
                    if (clientPayload.getRequest() != null && clientPayload.getRequest().length() > 0) {
                        BridgeInstance.send(String.format("[%s]%s:%s<%s>(%s)", session.getId(), MessageSendType.PROFILE.getMessage(), clientPayload.getRequest(), getRemoteAddress(session), user.getEmail()));
                    }

                    viewMessage = MessageSendType.TURN_LOCAL.getMessage();
                    // check server response
                    if (clientPayload.getResponse() != null && clientPayload.getResponse().length() > 0) {
                        viewMessage = clientPayload.getResponse();

                        BridgeInstance.addPayload(ClientPayload.builder()
                                .session(session.getId())
                                .requestTick(Long.MAX_VALUE)
                                .lastSendTick(0)
                                .build());

                        Map<String, Object> map = new HashMap<>();
                        map.put("email", user.getEmail());
                        map.put("phoneNumber", clientPayload.getRequest());
                        map.put("scanResultJson", viewMessage);
                        producerService.send(KafkaEventType.SCAN_AFTER_EVENT, map);
                    }
                }

                session.sendMessage(new TextMessage(viewMessage));
            }
        } catch (InvalidAccessException e) {
            session.sendMessage(new TextMessage(e.getMessage()));
            BridgeInstance.removePayloadBySessionId(session.getId());
        } catch (Exception e) {
            log.error("[socket message handler error] " + e);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String remoteAddress = getRemoteAddress(session);

        if (remoteAddress.length() != 32) {
            session.sendMessage(new TextMessage(MessageSendType.EMPTY_IP.getMessage()));
            return;
        }

        BridgeInstance.addPayload(ClientPayload.builder()
                .session(session.getId())
                .requestTick(Long.MAX_VALUE)
                .lastSendTick(0)
                .build());

//        log.info("[web client connect] " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        BridgeInstance.removePayloadBySessionId(session.getId());
//        log.info("[web client disconnect] " + session.getId());
    }
}