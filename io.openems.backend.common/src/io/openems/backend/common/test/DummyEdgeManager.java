package io.openems.backend.common.test;

import static io.openems.common.utils.CollectorUtils.toSortedMap;
import static java.util.Collections.emptySortedMap;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonElement;

import io.openems.backend.common.edge.EdgeCache;
import io.openems.backend.common.edge.EdgeManager;
import io.openems.backend.common.metadata.User;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.types.ChannelAddress;

public class DummyEdgeManager implements EdgeManager {

	private Map<String, SortedMap<ChannelAddress, JsonElement>> channelValues;

	public DummyEdgeManager(Map<String, SortedMap<ChannelAddress, JsonElement>> channelValues) {
		super();
		this.channelValues = channelValues;
	}

	@Override
	public CompletableFuture<JsonrpcResponseSuccess> send(//
			String edgeId, User user, JsonrpcRequest request //
	) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean send(String edgeId, JsonrpcNotification notification) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CompletableFuture<JsonrpcResponseSuccess> handleSubscribeSystemLogRequest(//
			String edgeId, User user, UUID websocketId, SubscribeSystemLogRequest request //
	) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> getChannelValues(//
			String edgeId, Set<ChannelAddress> channelAddresses //
	) {
		final var edgeChannels = this.channelValues.get(edgeId);
		if (edgeChannels == null) {
			return emptySortedMap();
		}

		return edgeChannels.entrySet().stream() //
				.filter(t -> channelAddresses.contains(t.getKey())) //
				.collect(toSortedMap(Entry::getKey, Entry::getValue));
	}

	@Override
	public EdgeCache getEdgeCacheForEdgeId(String edgeId) {
		throw new UnsupportedOperationException();
	}
}
