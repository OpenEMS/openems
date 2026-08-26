package io.openems.edge.system.fenecon.home;

import static io.openems.edge.common.component.OpenemsComponent.ChannelId.STATE;
import static io.openems.edge.common.test.TestUtils.withValue;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.channel.Level;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.api.backend.dummy.DummyControllerApiBackend;
import io.openems.edge.io.test.DummyCustomInputOutput;
import io.openems.edge.system.fenecon.home.enums.LedOrder;
import io.openems.edge.system.fenecon.home.enums.StateLed;

public class SystemFeneconHomeImplTest {

	private final DummyControllerApiBackend backend = new DummyControllerApiBackend("ctrlbackend0");
	private final DummySum sum = new DummySum();

	@Test
	public void testDetermineStateLed() {
		this.updateValues(Level.OK, true);
		assertEquals(StateLed.BLUE, StateLed.determineFrom(this.sum, this.backend));
		this.updateValues(Level.WARNING, false);
		assertEquals(StateLed.BLUE_DOTTED, StateLed.determineFrom(this.sum, this.backend));
	}

	private void updateValues(Level newSumStateValue, boolean newBackendConnected) {
		withValue(this.sum, STATE, newSumStateValue);
		this.backend.setConnected(newBackendConnected);
	}

	@Test
	public void testProcess() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final var io = new DummyCustomInputOutput("io1", "Digital_Output", 1, 3);
		final var componentManager = new DummyComponentManager(clock);
		componentManager.addComponent(io);
		new ComponentTest(new SystemFeneconHomeImpl()) //
				.addReference("componentManager", componentManager) //
				.addReference("sum", this.sum) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("backend", this.backend) //
				.addComponent(io) //
				.activate(MyConfig.create() //
						.setId("system0") //
						.setRelayId("io1") //
						.setLedOrder(LedOrder.DEFAULT_RED_BLUE_GREEN) //
						.build()) //
				.next(new TestCase() //
						.timeleap(clock, 1, SECONDS) //
						.input("_sum", STATE, Level.OK) //
						.onAfterProcessImage(() -> this.backend.setConnected(true)) //
						.input("io1", io.digitalOutputChannels()[1].address().getChannelId(), false) //
						.output("io1", io.digitalOutputChannels()[1].address().getChannelId(), true)) //
				.next(new TestCase() //
						.timeleap(clock, 1, SECONDS) //
						.input("_sum", STATE, Level.WARNING) //
						.onAfterProcessImage(() -> this.backend.setConnected(false)) //
						.input("io1", io.digitalOutputChannels()[1].address().getChannelId(), true) //
						.output("io1", io.digitalOutputChannels()[1].address().getChannelId(), true)) //
				.next(new TestCase() //
						.timeleap(clock, 1, SECONDS) //
						.input("_sum", STATE, Level.WARNING) //
						.onAfterProcessImage(() -> this.backend.setConnected(false)) //
						.input("io1", io.digitalOutputChannels()[1].address().getChannelId(), true) //
						.output("io1", io.digitalOutputChannels()[1].address().getChannelId(), false)) //
				.next(new TestCase() //
						.timeleap(clock, 1, SECONDS) //
						.input("_sum", STATE, Level.WARNING) //
						.onAfterProcessImage(() -> this.backend.setConnected(false)) //
						.input("io1", io.digitalOutputChannels()[1].address().getChannelId(), false) //
						.output("io1", io.digitalOutputChannels()[1].address().getChannelId(), true)) //
				.next(new TestCase() //
						.timeleap(clock, 1, SECONDS) //
						.input("_sum", STATE, Level.WARNING) //
						.onAfterProcessImage(() -> this.backend.setConnected(true)) //
						.input("io1", io.digitalOutputChannels()[1].address().getChannelId(), true) //
						.output("io1", io.digitalOutputChannels()[1].address().getChannelId(), false)) //
				.deactivate(); //
	}
}
