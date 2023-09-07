package io.openems.edge.ess.generic.offgrid.statemachine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.offgrid.api.OffGridSwitch;

public class ContextTest {

	private static class DummyOffGridSwitch extends AbstractOpenemsComponent implements OffGridSwitch {

		public DummyOffGridSwitch(String id) {
			this(id, new io.openems.edge.common.channel.ChannelId[0]);
		}

		public DummyOffGridSwitch(String id, io.openems.edge.common.channel.ChannelId[] additionalChannelIds) {
			super(//
					OpenemsComponent.ChannelId.values(), //
					OffGridSwitch.ChannelId.values(), //
					additionalChannelIds //
			);
			for (Channel<?> channel : this.channels()) {
				channel.nextProcessImage();
			}
			super.activate(null, id, "", true);
		}

		/**
		 * Sets and applies the {@link OffGridSwitch.ChannelId#MAIN_CONTACTOR}.
		 *
		 * @param value the state of the MainContactor
		 * @return myself
		 */
		public DummyOffGridSwitch withMainContactor(boolean value) {
			this._setMainContactor(value);
			this.getMainContactorChannel().nextProcessImage();
			return this;
		}

		/**
		 * Sets and applies the {@link OffGridSwitch.ChannelId#GROUNDING_CONTACTOR}.
		 *
		 * @param value the state of the GroundingContactor
		 * @return myself
		 */
		public DummyOffGridSwitch withGroundingContactor(boolean value) {
			this._setGroundingContactor(value);
			this.getGroundingContactorChannel().nextProcessImage();
			return this;
		}

		@Override
		public void setMainContactor(Contactor operation) throws IllegalArgumentException, OpenemsNamedException {
		}

		@Override
		public void setGroundingContactor(Contactor operation) throws IllegalArgumentException, OpenemsNamedException {
		}

	}

	@Test
	public void testIsOnGridContactorsSet() {
		var sut = new DummyOffGridSwitch("offGridSwitch0");
		var context = new Context(null, null, null, sut, null, null);

		sut.withMainContactor(false);
		sut.withGroundingContactor(false);
		assertTrue(context.isOnGridContactorsSet());

		sut.withMainContactor(true);
		sut.withGroundingContactor(false);
		assertFalse(context.isOnGridContactorsSet());

		sut.withMainContactor(false);
		sut.withGroundingContactor(true);
		assertFalse(context.isOnGridContactorsSet());

		sut.withMainContactor(true);
		sut.withGroundingContactor(true);
		assertFalse(context.isOnGridContactorsSet());
	}

	@Test
	public void testIsOffGridContactorsSet() {
		var sut = new DummyOffGridSwitch("offGridSwitch0");
		var context = new Context(null, null, null, sut, null, null);

		sut.withMainContactor(false);
		sut.withGroundingContactor(false);
		assertFalse(context.isOffGridContactorsSet());

		sut.withMainContactor(true);
		sut.withGroundingContactor(false);
		assertFalse(context.isOffGridContactorsSet());

		sut.withMainContactor(false);
		sut.withGroundingContactor(true);
		assertFalse(context.isOffGridContactorsSet());

		sut.withMainContactor(true);
		sut.withGroundingContactor(true);
		assertTrue(context.isOffGridContactorsSet());
	}

}
