package io.openems.edge.battery.bmw;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.osgi.service.event.Event;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.battery.bmw.enums.BatteryState;
import io.openems.edge.battery.bmw.statemachine.StateMachine;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpMethod;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttp;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class BmwBatteryImplTest {

	private static final String BATTERY_ID = "battery1";
	private static final String MODBUS_ID = "modbus0";
	private static final ChannelAddress STATEMACHINE = new ChannelAddress(BATTERY_ID, "StateMachine");
	private static final ChannelAddress BATTERY_STATE = new ChannelAddress(BATTERY_ID, "BatteryState");

	private static enum Operation {
		POWER_STATE, RELEASE_STATE
	}

	@Test
	public void startBattery() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final var fetcher = DummyBridgeHttpFactory.dummyEndpointFetcher();
		final var executor = DummyBridgeHttpFactory.dummyBridgeHttpExecutor(clock, true);
		final var dataState = new AtomicReference<String>("{data: \"0\"}");
		final var dataRelease = new AtomicReference<String>("{data: \"0\"}");

		fetcher.addEndpointHandler(t -> {

			var operation = t.url().contains("bcsPowerState") //
					? Operation.POWER_STATE //
					: Operation.RELEASE_STATE;

			if (t.body() == "{userCredentials: {name: \"foo\", password: \"foo_Password\"}}") {
				return HttpResponse.ok("token");
			}

			if (t.method() == HttpMethod.GET) {
				return switch (operation) {
				case POWER_STATE -> HttpResponse.ok(dataState.get());
				case RELEASE_STATE -> HttpResponse.ok(dataRelease.get());
				};
			}
			if (t.method() == HttpMethod.POST) {
				switch (operation) {
				case POWER_STATE -> {
					try {
						var jsonElement = JsonUtils.parse(t.body());
						var update = jsonElement.getAsJsonObject().get("data");
						dataState.set(update.toString());
						return HttpResponse.ok("");
					} catch (OpenemsNamedException e) {
						throw HttpError.ResponseError.notFound();
					}
				}
				case RELEASE_STATE -> {
					try {
						var jsonElement = JsonUtils.parse(t.body());
						var update = jsonElement.getAsJsonObject().get("data");
						dataRelease.set(update.toString());
						return HttpResponse.ok("");
					} catch (OpenemsNamedException e) {
						throw HttpError.ResponseError.notFound();
					}
				}
				}
				;
			}
			throw HttpError.ResponseError.notFound();
		});

		var dummyCycleSubscriber = DummyBridgeHttpFactory.cycleSubscriber();

		var test = new ComponentTest(new BatteryBmwImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("oem", new DummyOpenemsEdgeOem()) //
				.addReference("token", new BmwToken(new DummyBridgeHttp()))//
				.addReference("httpBridgeFactory", DummyBridgeHttpFactory.ofBridgeImpl(//
						() -> dummyCycleSubscriber, //
						() -> fetcher, //
						() -> executor)) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)//
						.withIpAddress("127.0.0.1")) //
				.activate(MyConfig.create() //
						.setId(BATTERY_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(1) //
						.setStartStop(StartStopConfig.START) //
						.build());//

		test.next(new TestCase("1")//
				.output(STATEMACHINE, StateMachine.State.UNDEFINED));
		test.next(new TestCase("2")//
				.onAfterProcessImage(() -> {
					dummyCycleSubscriber.handleEvent(
							new Event(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, Collections.emptyMap()));
				}));
		test.next(new TestCase("3")//
				.timeleap(clock, 10, ChronoUnit.SECONDS));//
		test.next(new TestCase("4")//
				.onAfterProcessImage(() -> {
					dummyCycleSubscriber.handleEvent(
							new Event(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, Collections.emptyMap()));
				}));
		test.next(new TestCase("5")//
				.output(STATEMACHINE, StateMachine.State.GO_RUNNING));
		test.next(new TestCase("6")//
				.onAfterProcessImage(() -> {
					dummyCycleSubscriber.handleEvent(
							new Event(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, Collections.emptyMap()));
				}));
		test.next(new TestCase("7")//
				.timeleap(clock, 10, ChronoUnit.SECONDS));//
		test.next(new TestCase("8")//
				.onAfterProcessImage(() -> {
					dummyCycleSubscriber.handleEvent(
							new Event(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, Collections.emptyMap()));
				}));
		test.next(new TestCase("9")//
				.input(BATTERY_STATE, BatteryState.OPERATION));
		test.next(new TestCase("10")//
				.onAfterProcessImage(() -> {
					dummyCycleSubscriber.handleEvent(
							new Event(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, Collections.emptyMap()));
				}));
		test.next(new TestCase("11")//
				.timeleap(clock, 10, ChronoUnit.SECONDS));//
		test.next(new TestCase("12")//
				.onAfterProcessImage(() -> {
					dummyCycleSubscriber.handleEvent(
							new Event(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, Collections.emptyMap()));
				}));
		test.next(new TestCase("13")//
				.output(STATEMACHINE, StateMachine.State.RUNNING));
	}
}
