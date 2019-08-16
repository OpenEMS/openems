package io.openems.backend.uiwebsocket.impl;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class OnClose implements io.openems.common.websocket.OnClose {

    private final Logger log = LoggerFactory.getLogger(OnClose.class);
    private final UiWebsocketImpl parent;

    public OnClose(UiWebsocketImpl parent) {
        this.parent = parent;
    }

    @Override
    public void run(WebSocket ws, int code, String reason, boolean remote) {
        WsData wsData = ws.getAttachment();
        wsData.getToken().ifPresent(token -> {
            Optional<String> userName = this.parent.accessControl.getUsernameForToken(token);
            if (userName.isPresent()) {
                this.parent.logInfo(this.log, "User [" + userName.get() + "] disconnected.");
            } else {
                this.parent.logInfo(this.log, "Unknown user disconnected.");
            }
        });

        wsData.dispose();
    }

}
