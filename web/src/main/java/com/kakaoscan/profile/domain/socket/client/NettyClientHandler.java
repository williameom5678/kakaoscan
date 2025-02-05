package com.kakaoscan.profile.domain.socket.client;

import com.kakaoscan.profile.domain.socket.common.BridgeInstance;
import com.kakaoscan.profile.domain.socket.common.ClientPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

@Log4j2
@RequiredArgsConstructor
@Component
@ChannelHandler.Sharable
public class NettyClientHandler extends ChannelInboundHandlerAdapter {
    private static final int REQUEST_TIMEOUT_TICK = 3 * 1000;

    private final NettyClientInstance nettyClientInstance;

    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (!nettyClientInstance.isConnected()) {
            nettyClientInstance.setStartTime(System.currentTimeMillis()); //서버 접속 tick
        }
    }
    /**
     * 서버에서 메세지 수신
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf)msg;
        String receivedMessage = buf.toString(Charset.forName("euc-kr"));
//        log.info("[received] " + receivedMessage);

        try {
            String[] s = receivedMessage.split(":", 2);
            String wsSession = s[0];
            String jsonMessage = s[1];
            // check session

            ClientPayload clientPayload = BridgeInstance.getPayloadBySessionId(wsSession);

            // response
            clientPayload.setLastReceivedTick(System.currentTimeMillis() + REQUEST_TIMEOUT_TICK);
            if (jsonMessage!= null && jsonMessage.length() > 0) {
                clientPayload.setResponse(jsonMessage);
                clientPayload.setTryConnect(true);
                clientPayload.setConnectFail(false);
                BridgeInstance.clear();
            }

            BridgeInstance.addPayload(clientPayload);
        }catch (NullPointerException e) {
            ctx.close();
        }finally {
            buf.release();
        }
    }

    /**
     * 이벤트 상태
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws UnsupportedEncodingException {
        if (!(evt instanceof IdleStateEvent)) {
            return;
        }

        if (BridgeInstance.getMessage().length() == 0) {
            return;
        }

        IdleStateEvent e = (IdleStateEvent) evt;
        if (e.state() == IdleState.WRITER_IDLE) {
            byte[] buffer = BridgeInstance.getMessage().getBytes(Charset.forName("euc-kr"));
            String decodeString = new String(buffer, "euc-kr");

            ctx.writeAndFlush(Unpooled.copiedBuffer(decodeString, Charset.forName("euc-kr")));
        }

        if (e.state() == IdleState.READER_IDLE) {
            ctx.close();
            BridgeInstance.clear();
        }
    }

    /**
     * 예외 발생
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error(cause);
        ctx.close();
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
        nettyClientInstance.setStartTime(-1);
//        ctx.channel().eventLoop().schedule(() -> nettyInstance.connect(), RECONNECT_DELAY, TimeUnit.SECONDS);
    }
}
