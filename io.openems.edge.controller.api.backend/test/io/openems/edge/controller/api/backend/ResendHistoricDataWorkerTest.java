package io.openems.edge.controller.api.backend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.controller.api.backend.ResendHistoricDataWorker.TriggerState;

public class ResendHistoricDataWorkerTest {

	@Test
	public void testState() throws Throwable {
		final var worker = new ResendHistoricDataWorker();

		assertEquals(TriggerState.INIT, worker.triggerState.get());
		worker.triggerNextRun();
		assertEquals(TriggerState.AFTER_TRIGGER, worker.triggerState.get());
		worker.forever();
		assertEquals(TriggerState.SKIP_FIRST_FOREVER, worker.triggerState.get());
		assertTrue(worker.getCycleTime() >= ResendHistoricDataWorker.DELAY_TRIGGER_TIME);
	}

	@Test
	public void testMapResendData() {
		final var testTimestamp = 1L;
		final var testChannel = new ChannelAddress("c1", "test");
		final var testValue = new JsonPrimitive("value");
		SortedMap<Long, SortedMap<ChannelAddress, JsonElement>> data = new TreeMap<>();
		data.computeIfAbsent(testTimestamp, a -> new TreeMap<>()) //
				.put(testChannel, testValue);
		final var mapped = ResendHistoricDataWorker.mapResendData(data);
		assertEquals(testValue, mapped.get(testTimestamp, testChannel.toString()));
	}

}
