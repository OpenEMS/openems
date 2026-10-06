package io.openems.edge.bridge.eebus.api;

import java.net.InetSocketAddress;
import java.time.Instant;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

public record EebusConnectionInfo(//
		ConnectionType connectionType, //
		String ski, //
		InetSocketAddress socketAddress, //
		Integer trustLevel, //
		boolean isDataExchangeEstablished, //
		Instant connectionStartDate //
) {

	public static JsonSerializer<EebusConnectionInfo> serializer() {
		return JsonSerializerUtil.jsonObjectSerializer(EebusConnectionInfo.class, json -> {
			return new EebusConnectionInfo(//
					json.getEnum("connectionType", ConnectionType.class), //
					json.getString("ski"), //
					InetSocketAddress.createUnresolved(json.getString("socket_host"), json.getInt("socket_port")), //
					json.getInt("trustLevel"), //
					json.getBoolean("isDataExchangeEstablished"), //
					json.getInstant("connectionStartDate"));
		}, obj -> {
			return JsonUtils.buildJsonObject() //
					.addProperty("connectionType", obj.connectionType()) //
					.addProperty("ski", obj.ski()) //
					.addProperty("socket_host", obj.socketAddress().getHostString()) //
					.addProperty("socket_port", obj.socketAddress().getPort()) //
					.addProperty("trustLevel", obj.trustLevel()) //
					.addProperty("isDataExchangeEstablished", obj.isDataExchangeEstablished()) //
					.addProperty("connectionStartDate", obj.connectionStartDate()) //
					.build();
		});
	}

	public enum ConnectionType {
		CLIENT_CONNECTION_TO_PEER, //
		PEER_CONNECTED_TO_SERVER;
	}

}
