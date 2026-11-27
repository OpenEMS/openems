package io.openems.backend.edge.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.Closeable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.backend.common.edge.jsonrpc.ConnectedEdges;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.test.DummyMetadata;
import io.openems.backend.common.timedata.TimedataManager;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.notification.AggregatedDataNotification;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.types.DataTable;
import io.openems.common.types.SemanticVersion;
import io.openems.common.utils.FunctionUtils;

public class OnNotificationTest {

	private static final Logger LOG = LoggerFactory.getLogger(OnNotificationTest.class);
	private static final Metadata METADATA = new DummyMetadata(e -> LOG.info(e.toString()));
	private static final String ID = "test";

	private static OnNotification getOnNotification(TimedataManager timedataManager, //
			Function<String, Optional<Edge>> getEdge, //
			Function<String, BiConsumer<Double, String>> dummyMetricsConsumer) {
		final var onNotification = new OnNotification("test", //
				FunctionUtils::returnNothing, //
				FunctionUtils::returnNothing, //
				() -> timedataManager, //
				getEdge, //
				FunctionUtils::doNothing, //
				FunctionUtils::doNothing, //
				FunctionUtils::doNothing, //
				FunctionUtils::doNothing);

		// Mock the environment, as Prometheus metrics are not available in unit tests
		final var spyOnNotification = spy(onNotification);
		doReturn(mock(Closeable.class)).when(spyOnNotification).startTimer(anyString());
		doAnswer(invocation -> dummyMetricsConsumer.apply(invocation.getArgument(0))) //
				.when(spyOnNotification).getMetricsConsumer(anyString());

		return spyOnNotification;
	}

	private ConcurrentHashMap<String, Edge> edges;
	private ConcurrentHashMap<String, Double> edgeMetrics;
	private DummyTimedataManager timedataManager;
	private DummyWebsocket websocket;
	private OnNotification onNotification;

	@BeforeEach
	public void setUp() {
		this.edges = new ConcurrentHashMap<>();
		this.edgeMetrics = new ConcurrentHashMap<>();
		this.timedataManager = new DummyTimedataManager();
		this.websocket = new DummyWebsocket(ws -> {
			final var wsData = new WsData(ws);
			wsData.setId(ID);
			return wsData;
		});
		this.onNotification = getOnNotification(//
				this.timedataManager, //
				edgeId -> Optional.ofNullable(this.edges.get(edgeId)), //
				metric -> (value, label) -> this.edgeMetrics.put(metric + "-" + label, value));
	}

	@Test
	public void testHandleAggregatedDataNotification() throws OpenemsNamedException {
		final var edgeId = "edge1";
		final var edge = new Edge(METADATA, edgeId, "", "", "", null);
		this.edges.put(edgeId, edge);

		final var stateToSet = Level.FAULT;
		final var versionToSet = new SemanticVersion(1, 2, 3);

		final var data = new DataTable();
		data.put(0L, "_sum/State", new JsonPrimitive(Level.OK.ordinal()));
		data.put(0L, "_meta/Version", new JsonPrimitive("4.2.0"));
		data.put(10L, "_sum/State", new JsonPrimitive(Level.WARNING.ordinal()));
		data.put(20L, "_meta/Version", new JsonPrimitive(versionToSet.toString()));
		data.put(100L, "_sum/State", new JsonPrimitive(stateToSet.ordinal()));
		data.put(10L, "_meta/Version", new JsonPrimitive("4.2.0"));

		final var notification = new AggregatedDataNotification(data);
		final var rpcNotification = new EdgeRpcNotification(edgeId, notification);

		this.websocket.getWsData().updateEdgeStatus(edgeId, true);

		this.onNotification.accept(this.websocket, rpcNotification);

		assertEquals(versionToSet, edge.getVersion());
		assertEquals(stateToSet, edge.getSumState());
		assertEquals(1, this.timedataManager.get(edgeId).size());
	}

	@Test
	public void testHandleAggregatedDataNotification_withoutVersionOrState() throws OpenemsNamedException {
		final var edgeId = "edge2";
		final var edge = new Edge(METADATA, edgeId, "", "", "", null);
		this.edges.put(edgeId, edge);

		final var data = new DataTable();
		data.put(0L, "component1/Channel0", new JsonPrimitive(1));
		data.put(1L, "_sum/State", JsonNull.INSTANCE);

		final var notification = new AggregatedDataNotification(data);
		final var rpcNotification = new EdgeRpcNotification(edgeId, notification);

		this.websocket.getWsData().updateEdgeStatus(edgeId, true);

		this.onNotification.accept(this.websocket, rpcNotification);
		assertEquals(SemanticVersion.ZERO, edge.getVersion());
		assertNull(edge.getSumState());
		assertEquals(1, this.timedataManager.get(edgeId).size());
	}

	@Test
	public void testHandleAggregatedDataNotification_nullLatestStateKeepsEarlierState() throws OpenemsNamedException {
		final var edgeId = "edge4";
		final var edge = new Edge(METADATA, edgeId, "", "", "", null);
		this.edges.put(edgeId, edge);

		final var data = new DataTable();
		data.put(0L, "_sum/State", new JsonPrimitive(Level.WARNING.ordinal()));
		data.put(10L, "_sum/State", JsonNull.INSTANCE);

		this.websocket.getWsData().updateEdgeStatus(edgeId, true);
		this.onNotification.accept(this.websocket,
				new EdgeRpcNotification(edgeId, new AggregatedDataNotification(data)));

		assertEquals(Level.WARNING, edge.getSumState());
	}

	@Test
	public void testHandleAggregatedDataNotification_unknownStateOrdinalMapsToFault() throws OpenemsNamedException {
		final var edgeId = "edge5";
		final var edge = new Edge(METADATA, edgeId, "", "", "", null);
		this.edges.put(edgeId, edge);

		final var data = new DataTable();
		data.put(0L, "_sum/State", new JsonPrimitive(Level.OK.ordinal()));
		data.put(10L, "_sum/State", new JsonPrimitive(99));

		this.websocket.getWsData().updateEdgeStatus(edgeId, true);
		this.onNotification.accept(this.websocket,
				new EdgeRpcNotification(edgeId, new AggregatedDataNotification(data)));

		assertEquals(Level.FAULT, edge.getSumState());
	}

	@Test
	public void testHandleTimestampedDataNotification_withoutEdgeOrEdgeCache() throws OpenemsNamedException {
		final var edgeId = "edge3";
		final var channel = "component1/Channel0";

		final var data = new DataTable();
		data.put(0L, channel, new JsonPrimitive(1));

		this.websocket.getWsData().updateEdgeStatus(edgeId, false);
		final var rpcNotificationNoCache = new EdgeRpcNotification(edgeId, new TimestampedDataNotification(data));

		this.onNotification.accept(this.websocket, rpcNotificationNoCache);
		assertFalse(this.websocket.getWsData().containsEdgeId(edgeId));

		this.websocket.getWsData().updateEdgeStatus(edgeId, true);
		final var rpcNotificationNoEdge = new EdgeRpcNotification(edgeId, new TimestampedDataNotification(data));

		this.onNotification.accept(this.websocket, rpcNotificationNoEdge);
		assertEquals(new JsonPrimitive(1),
				this.websocket.getWsData().getEdgeClient(edgeId).getCache().getChannelValue(channel));

		assertEquals(1, this.timedataManager.get(edgeId).size());
	}

	@Test
	public void testHandleConnectedEdgesNotification() throws OpenemsNamedException {
		final var edgeIds = Set.of("new1", "new2");
		final var metrics = Map.<String, Number>of(//
				"Pending", 5, //
				"Active", 3, //
				"Completed", 10, //
				"PoolSize", 8, //
				"MaxPoolSize", 12);
		final var notification = new ConnectedEdges.Notification(edgeIds, metrics);

		this.onNotification.accept(this.websocket, notification);

		assertEquals(5.0, this.edgeMetrics.get("Pending-" + ID));
		assertEquals(3.0, this.edgeMetrics.get("Active-" + ID));
		assertEquals(10.0, this.edgeMetrics.get("Completed-" + ID));
		assertEquals(8.0, this.edgeMetrics.get("PoolSize-" + ID));
		assertEquals(12.0, this.edgeMetrics.get("MaxPoolSize-" + ID));

		assertTrue(this.websocket.getWsData().containsEdgeId("new1"));
		assertTrue(this.websocket.getWsData().containsEdgeId("new2"));

		assertEquals(1, this.timedataManager.get("backend0").size());
	}

}
