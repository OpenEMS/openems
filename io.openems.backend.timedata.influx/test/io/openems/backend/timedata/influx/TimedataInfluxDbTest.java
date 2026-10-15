package io.openems.backend.timedata.influx;

import static io.openems.common.utils.ReflectionUtils.setAttributeViaReflection;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.influxdb.client.write.Point;

import io.openems.common.jsonrpc.notification.AggregatedDataNotification;
import io.openems.common.jsonrpc.notification.ResendDataNotification;
import io.openems.common.oem.DummyOpenemsBackendOem;
import io.openems.shared.influxdb.InfluxConnector;

public class TimedataInfluxDbTest {

	private static final String EDGE_ID = "edge0";
	private static final String CHANNEL = "ess0/ActivePower";
	private static final long TIMESTAMP = 1577836800000L;

	@Test
	public void testWriteResendDataNotification() throws Exception {
		final var influxConnector = mock(InfluxConnector.class);
		final var sut = createTimedataInfluxDb(false, influxConnector);

		sut.write(EDGE_ID, createResendDataNotification());
		sut.write(EDGE_ID, createAggregatedDataNotification());

		final var captor = ArgumentCaptor.forClass(Point.class);
		verify(influxConnector, times(2)).write(captor.capture());

		final var resendPoint = captor.getAllValues().get(0);
		assertTrue(resendPoint.hasFields());
		assertEquals("0", resendPoint.getTags().get("edge"));
		assertEquals(123L, resendPoint.getFields().get(CHANNEL));
		assertFalse(captor.getAllValues().get(1).hasFields());
	}

	@Test
	public void testWriteResendDataNotificationInReadOnlyMode() throws Exception {
		final var influxConnector = mock(InfluxConnector.class);
		final var sut = createTimedataInfluxDb(true, influxConnector);

		sut.write(EDGE_ID, createResendDataNotification());

		verify(influxConnector, never()).write(any(Point.class));
	}

	private static TimedataInfluxDb createTimedataInfluxDb(boolean isReadOnly, InfluxConnector influxConnector)
			throws Exception {
		final var sut = new TimedataInfluxDb();
		setAttributeViaReflection(sut, "config", createConfig(isReadOnly));
		setAttributeViaReflection(sut, "timeFilter", TimeFilter.from("", ""));
		setAttributeViaReflection(sut, "channelFilter", ChannelFilter.from(new String[0], new String[0]));
		setAttributeViaReflection(sut, "influxConnector", influxConnector);
		setAttributeViaReflection(sut, "oem", new DummyOpenemsBackendOem());
		return sut;
	}

	private static Config createConfig(boolean isReadOnly) {
		final var config = mock(Config.class);
		when(config.isReadOnly()).thenReturn(isReadOnly);
		when(config.measurement()).thenReturn("data");
		when(config.blacklistedChannels()).thenReturn(new String[0]);
		when(config.blacklistedChannelIds()).thenReturn(new String[0]);
		return config;
	}

	private static ResendDataNotification createResendDataNotification() {
		final var data = TreeBasedTable.<Long, String, JsonElement>create();
		data.put(TIMESTAMP, CHANNEL, new JsonPrimitive(123));
		return new ResendDataNotification(data);
	}

	private static AggregatedDataNotification createAggregatedDataNotification() {
		final var data = TreeBasedTable.<Long, String, JsonElement>create();
		data.put(TIMESTAMP, CHANNEL, new JsonPrimitive(456));
		return new AggregatedDataNotification(data);
	}
}
