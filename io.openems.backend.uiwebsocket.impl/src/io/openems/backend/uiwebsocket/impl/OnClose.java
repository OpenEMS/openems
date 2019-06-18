package io.openems.backend.uiwebsocket.impl;

import java.util.Optional;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.metadata.api.BackendUser;
import io.openems.common.exceptions.OpenemsException;

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
            String userName = this.parent.accessControl.getUsernameForToken(token);
            this.parent.logInfo(this.log, "User [" + userName + "] disconnected.");
        });

        wsData.dispose();
    }

}
