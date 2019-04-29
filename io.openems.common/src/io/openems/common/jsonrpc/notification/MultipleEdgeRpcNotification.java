package io.openems.common.jsonrpc.notification;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This Notification wraps a list of {@link EdgeRpcNotification} and returns those as a JsonArray for sending it to the
 * UI
 */
public class MultipleEdgeRpcNotification extends JsonrpcNotification {

    public final static String METHOD = "multipleEdgeRpc";
    private final List<EdgeRpcNotification> edgeRpcNotifications = new ArrayList<>();

    private final Logger log = LoggerFactory.getLogger(MultipleEdgeRpcNotification.class);

    public MultipleEdgeRpcNotification() {
        super(METHOD);
    }

    public void addEdgeRpcNotification(EdgeRpcNotification edgeRpcNotification) {
        this.edgeRpcNotifications.add(edgeRpcNotification);
    }

    @Override
    public JsonObject getParams() {
        /*JsonArray retVal = new JsonArray();
        try {
            for (EdgeRpcNotification notification : this.edgeRpcNotifications) {
                retVal.add(JsonUtils.getAsJsonObject(notification.getParams()));
            }

            return retVal;
        } catch (Exception e) {
            log.error("Parsing the edgeRpcNotifications did not work", e.getMessage());
        }*/

        return null;
    }
}
