package io.openems.edge.controller.api.websocket;

import java.util.Optional;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.user.EdgeUser;

public class OnClose implements io.openems.common.websocket.OnClose {

    private final Logger log = LoggerFactory.getLogger(OnClose.class);
    private final WebsocketApi parent;

    public OnClose(WebsocketApi parent) {
        this.parent = parent;
    }

    @Override
    public void run(WebSocket ws, int code, String reason, boolean remote) throws OpenemsException {
        WsData wsData = ws.getAttachment();
        if (wsData.getSessionToken() != null) {
            String userName = this.parent.accessControl.getUsernameForToken(wsData.getSessionToken());
            this.parent.logInfo(this.log, "User [" + userName + "] disconnected.");
        }

        wsData.dispose();
    }

}
