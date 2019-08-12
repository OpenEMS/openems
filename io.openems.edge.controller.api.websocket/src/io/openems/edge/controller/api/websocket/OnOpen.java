package io.openems.edge.controller.api.websocket;

import java.util.Optional;
import java.util.UUID;

import io.openems.common.accesscontrol.AuthenticationException;
import io.openems.common.accesscontrol.RoleId;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.notification.AuthenticateWithSessionIdFailedNotification;
import io.openems.common.jsonrpc.notification.AuthenticateWithSessionIdNotification;

public class OnOpen implements io.openems.common.websocket.OnOpen {

    private final Logger log = LoggerFactory.getLogger(OnOpen.class);
    private final WebsocketApi parent;

    OnOpen(WebsocketApi parent) {
        this.parent = parent;
    }

    @Override
    public void run(WebSocket ws, JsonObject handshake) {
        // get websocket attachment
        WsData wsData = ws.getAttachment();

        // get token from cookie or generate new token
        Optional<String> cookieTokenOpt = io.openems.common.websocket.OnOpen.getFieldFromHandshakeCookie(handshake,
                "token");

        // get token from cookie or generate new token
        cookieTokenOpt.ifPresent(cookieToken -> {
            try {
                // read token from Cookie
                UUID token = UUID.fromString(cookieToken);

                // login using token from the cookie
                RoleId roleId = this.parent.accessControl.login(token);

                // token from cookie is valid -> authentication successful
                // store user in attachment
                wsData.setRoleId(roleId);
                // send authentication notification
                AuthenticateWithSessionIdNotification notification = new AuthenticateWithSessionIdNotification(
                        token, Utils.getEdgeMetadata(wsData.getRoleId()));
                this.parent.server.sendMessage(ws, notification);

                // log
                Optional<String> userName = this.parent.accessControl.getUsernameForToken(token);
                if (userName.isPresent()) {
                    this.parent.logInfo(this.log, "User [" + userName.get() + "] logged in by token");
                } else {
                    this.parent.logInfo(this.log, "Unknown user logged in by token (" + token + ").");
                }
            } catch (IllegalArgumentException e) {
                this.parent.logWarn(this.log, "Cookie Token [" + cookieToken + "] is not a UUID: " + e.getMessage());
            } catch (AuthenticationException e) {
                // automatic authentication was not possible -> notify client
                this.parent.server.sendMessage(ws, new AuthenticateWithSessionIdFailedNotification());
            }
        });
    }

}
