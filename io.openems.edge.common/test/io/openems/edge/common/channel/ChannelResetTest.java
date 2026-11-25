package io.openems.edge.common.channel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

class ChannelResetTest {

	private enum TestChannelId implements io.openems.edge.common.channel.ChannelId {
		SUM(Doc.of(OpenemsType.INTEGER)), //
		INPUT_A(Doc.of(OpenemsType.INTEGER)), //
		INPUT_B(Doc.of(OpenemsType.INTEGER)), //
		READ_WRITE(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		WRITE_ONLY(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.WRITE_ONLY)), //
		CUMULATED_ENERGY(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.CUMULATED_WATT_HOURS));

		private final Doc doc;

		TestChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	private static final class TestComponent extends AbstractOpenemsComponent implements OpenemsComponent {

		private TestComponent() {
			super(OpenemsComponent.ChannelId.values(), TestChannelId.values());
			this.activate(null, "testComponent", "", true);
		}

		private IntegerReadChannel sum() {
			return this.channel(TestChannelId.SUM);
		}

		private IntegerReadChannel inputA() {
			return this.channel(TestChannelId.INPUT_A);
		}

		private IntegerReadChannel inputB() {
			return this.channel(TestChannelId.INPUT_B);
		}

		private IntegerWriteChannel readWrite() {
			return this.channel(TestChannelId.READ_WRITE);
		}

		private IntegerWriteChannel writeOnly() {
			return this.channel(TestChannelId.WRITE_ONLY);
		}

		private IntegerReadChannel cumulatedEnergy() {
			return this.channel(TestChannelId.CUMULATED_ENERGY);
		}

		/** Derives SUM from INPUT_A and INPUT_B via onSetNextValue-Callbacks. */
		private void deriveSumFromInputs() {
			this.inputA().onSetNextValue(_ -> this.updateSum());
			this.inputB().onSetNextValue(_ -> this.updateSum());
		}

		/** Sums up all defined inputs; UNDEFINED if no input is defined. */
		private void updateSum() {
			var definedInputs = Stream.of(this.inputA(), this.inputB()) //
					.map(channel -> channel.getNextValue().get()) //
					.filter(Objects::nonNull) //
					.toList();
			this.sum().setNextValue(definedInputs.isEmpty() //
					? null //
					: definedInputs.stream().mapToInt(Integer::intValue).sum());
		}

		/** Simulates the switch of the process image at the start of a Cycle. */
		private void nextCycle() {
			this.channels().forEach(Channel::nextProcessImage);
		}
	}

	@Test
	void resetToNull_takesEffectWithNextCycle() {
		var component = new TestComponent();
		var channel = component.inputA();
		channel.setNextValue(42);
		component.nextCycle();

		channel.resetToNull();

		assertEquals(Integer.valueOf(42), channel.value().get());
		component.nextCycle();
		assertFalse(channel.value().isDefined());
	}

	@Test
	void resetToNull_keepsPendingWriteValue() throws Exception {
		var channel = new TestComponent().writeOnly();
		channel.setNextWriteValue(10);

		channel.resetToNull();

		assertEquals(Integer.valueOf(10), channel.getNextWriteValue().get());
	}

	@Test
	void discardNextWriteValue_discardsPendingWriteValue() throws Exception {
		var channel = new TestComponent().writeOnly();
		channel.setNextWriteValue(10);

		channel.discardNextWriteValue();

		assertTrue(channel.getNextWriteValue().isEmpty());
	}

	@Test
	void resetAllChannelsToNull_resetsReadAndWriteChannels() {
		var component = new TestComponent();
		component.inputA().setNextValue(1);
		component.readWrite().setNextValue(2);
		component.nextCycle();

		component.resetAllChannelsToNull();
		component.nextCycle();

		assertFalse(component.inputA().value().isDefined());
		assertFalse(component.readWrite().value().isDefined());
	}

	@Test
	void resetAllChannelsToNull_keepsPendingWriteValues() throws Exception {
		var component = new TestComponent();
		component.readWrite().setNextWriteValue(1);
		component.writeOnly().setNextWriteValue(2);

		component.resetAllChannelsToNull();

		assertEquals(Integer.valueOf(1), component.readWrite().getNextWriteValue().get());
		assertEquals(Integer.valueOf(2), component.writeOnly().getNextWriteValue().get());
	}

	@Test
	void resetChannelsToNull_appliesFilter() {
		var component = new TestComponent();
		component.inputA().setNextValue(1);
		component.readWrite().setNextValue(2);
		component.nextCycle();

		component.resetChannelsToNull(channel -> !(channel instanceof WriteChannel));
		component.nextCycle();

		assertFalse(component.inputA().value().isDefined());
		assertEquals(Integer.valueOf(2), component.readWrite().value().get());
	}

	@Test
	void resetAllChannelsToNull_resetsDerivedChannelIndependentOfOrder() {
		var component = new TestComponent();
		component.deriveSumFromInputs();
		component.inputA().setNextValue(1);
		component.inputB().setNextValue(2);
		component.nextCycle();
		assertEquals(Integer.valueOf(3), component.sum().value().get());

		component.resetAllChannelsToNull();
		component.nextCycle();

		assertFalse(component.sum().value().isDefined());
	}

	@Test
	void resetAllChannelsToNull_continuesOnFailingCallback() {
		var component = new TestComponent();
		component.inputA().onSetNextValue(channelValue -> {
			if (!channelValue.isDefined()) {
				throw new IllegalStateException("Callback is not null-safe");
			}
		});
		component.inputA().setNextValue(1);
		component.inputB().setNextValue(2);
		component.nextCycle();

		component.resetAllChannelsToNull();
		component.nextCycle();

		assertFalse(component.inputB().value().isDefined());
	}

	@Test
	void discardAllNextWriteValues_discardsAllPendingWriteValues() throws Exception {
		var component = new TestComponent();
		component.readWrite().setNextWriteValue(1);
		component.writeOnly().setNextWriteValue(2);

		component.discardAllNextWriteValues();

		assertTrue(component.readWrite().getNextWriteValue().isEmpty());
		assertTrue(component.writeOnly().getNextWriteValue().isEmpty());
	}

	@Test
	void resetAllChannelsToNull_keepsCumulatedChannel() {
		var component = new TestComponent();
		component.cumulatedEnergy().setNextValue(100);
		component.nextCycle();

		component.resetAllChannelsToNull();
		component.nextCycle();

		assertEquals(Integer.valueOf(100), component.cumulatedEnergy().value().get());
	}

	@Test
	void resetAllChannelsToNull_keepsStateChannel() {
		var component = new TestComponent();
		component.getStateChannel().setNextValue(Level.WARNING.getValue());
		component.nextCycle();
		assertEquals(Level.WARNING, component.getState());

		component.resetAllChannelsToNull();
		component.nextCycle();

		assertEquals(Level.WARNING, component.getState());
	}
}
