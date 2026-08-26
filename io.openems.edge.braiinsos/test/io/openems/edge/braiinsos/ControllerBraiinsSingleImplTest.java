package io.openems.edge.braiinsos;

import static io.openems.common.test.TestUtils.createDummyClock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.braiinsos.api.BraiinsApi;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;

class ControllerBraiinsSingleImplTest {

	@Test
	void testRun_shouldSendResumeOnlyOnce_whenModeStaysOn() throws Exception {
		final var api = mock(BraiinsApi.class);
		when(api.callActionResume()).thenReturn(CompletableFuture.completedFuture(null));

		final var clock = createDummyClock();
		new ControllerTest(new ControllerBraiinsSingleImpl(() -> api))//
				.addReference("configurationAdmin", new DummyConfigurationAdmin())//
				.addReference("componentManager", new DummyComponentManager(clock))//
				.addReference("httpBridgeFactory", mock(BridgeHttpFactory.class))//
				.activate(MyConfig.create()//
						.setId("ctrlBraiinsSingle0")//
						.setIp("localhost")//
						.setMode(Mode.ON)//
						.build())//
				.next(new TestCase()//
						.output(ControllerBraiinsSingle.ChannelId.EFFECTIVE_MODE, Mode.ON))//
				.next(new TestCase()//
						.output(ControllerBraiinsSingle.ChannelId.EFFECTIVE_MODE, Mode.ON))//
				.deactivate();

		verify(api, times(1)).activate();
		verify(api, times(1)).deactivate();
		verify(api, times(1)).callActionResume();
		verify(api, never()).callActionPause();
	}

	@Test
	void testRun_ShouldSendPause_whenModeIsOff() throws Exception {
		final var api = mock(BraiinsApi.class);
		when(api.callActionPause()).thenReturn(CompletableFuture.completedFuture(null));

		final var clock = createDummyClock();
		new ControllerTest(new ControllerBraiinsSingleImpl(() -> api))//
				.addReference("configurationAdmin", new DummyConfigurationAdmin())//
				.addReference("componentManager", new DummyComponentManager(clock))//
				.addReference("httpBridgeFactory", mock(BridgeHttpFactory.class))//
				.activate(MyConfig.create()//
						.setId("ctrlBraiinsSingle0")//
						.setIp("localhost")//
						.setMode(Mode.OFF)//
						.build())//
				.next(new TestCase()//
						.output(ControllerBraiinsSingle.ChannelId.EFFECTIVE_MODE, Mode.OFF))//
				.deactivate();

		verify(api, times(1)).callActionPause();
		verify(api, never()).callActionResume();
	}

	@Test
	void testRun_ShouldRetry_whenFailedModeChange() throws Exception {
		final var api = mock(BraiinsApi.class);
		final var callCount = new AtomicInteger();
		when(api.callActionResume()).thenAnswer(ignore -> callCount.getAndIncrement() == 0 //
				? CompletableFuture.failedFuture(new RuntimeException("boom")) //
				: CompletableFuture.completedFuture(null));

		final var clock = createDummyClock();
		new ControllerTest(new ControllerBraiinsSingleImpl(() -> api))//
				.addReference("configurationAdmin", new DummyConfigurationAdmin())//
				.addReference("componentManager", new DummyComponentManager(clock))//
				.addReference("httpBridgeFactory", mock(BridgeHttpFactory.class))//
				.activate(MyConfig.create()//
						.setId("ctrlBraiinsSingle0")//
						.setIp("localhost")//
						.setMode(Mode.ON)//
						.build())//
				.next(new TestCase()//
						.output(ControllerBraiinsSingle.ChannelId.EFFECTIVE_MODE, Mode.ON))//
				.next(new TestCase()//
						.output(ControllerBraiinsSingle.ChannelId.EFFECTIVE_MODE, Mode.ON))//
				.deactivate();

		verify(api, times(2)).callActionResume();
	}

	@Test
	void testRun_shouldNotSendSecondResume_whenPreviousFutureIsPending() throws Exception {
		final var api = mock(BraiinsApi.class);
		final var pendingFuture = new CompletableFuture<Void>();
		when(api.callActionResume()).thenReturn(pendingFuture);

		final var clock = createDummyClock();
		new ControllerTest(new ControllerBraiinsSingleImpl(() -> api))//
				.addReference("configurationAdmin", new DummyConfigurationAdmin())//
				.addReference("componentManager", new DummyComponentManager(clock))//
				.addReference("httpBridgeFactory", mock(BridgeHttpFactory.class))//
				.activate(MyConfig.create()//
						.setId("ctrlBraiinsSingle0")//
						.setIp("localhost")//
						.setMode(Mode.ON)//
						.build())//
				.next(new TestCase()//
						.output(ControllerBraiinsSingle.ChannelId.EFFECTIVE_MODE, Mode.ON))//
				.next(new TestCase()//
						.output(ControllerBraiinsSingle.ChannelId.EFFECTIVE_MODE, Mode.ON))//
				.deactivate();

		verify(api, times(1)).callActionResume();
		verify(api, never()).callActionPause();
	}
}
