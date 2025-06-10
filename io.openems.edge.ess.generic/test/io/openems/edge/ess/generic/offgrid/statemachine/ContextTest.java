package io.openems.edge.ess.generic.offgrid.statemachine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.ess.offgrid.api.OffGridSwitch;

public class ContextTest {

	private static class DummyOffGridSwitch extends AbstractDummyOpenemsComponent<DummyOffGridSwitch>
			implements OffGridSwitch {

		public DummyOffGridSwitch(String id) {
			super(id, //
					OpenemsComponent.ChannelId.values(), //
					OffGridSwitch.ChannelId.values());
		}

		@Override
		protected DummyOffGridSwitch self() {
			return this;
		}

		/**
		 * Set {@link OffGridSwitch.ChannelId#MAIN_CONTACTOR}.
		 *
		 * @param value the value
		 * @return myself
		 */
		public DummyOffGridSwitch withMainContactor(boolean value) {
			TestUtils.withValue(this, OffGridSwitch.ChannelId.MAIN_CONTACTOR, value);
			return this.self();
		}

		/**
		 * Set {@link OffGridSwitch.ChannelId#GROUNDING_CONTACTOR}.
		 *
		 * @param value the value
		 * @return myself
		 */
		public DummyOffGridSwitch withGroundingContactor(boolean value) {
			TestUtils.withValue(this, OffGridSwitch.ChannelId.GROUNDING_CONTACTOR, value);
			return this.self();
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

		sut //
				.withMainContactor(false) //
				.withGroundingContactor(false);
		assertTrue(context.isOnGridContactorsSet());

		sut //
				.withMainContactor(true) //
				.withGroundingContactor(false);
		assertFalse(context.isOnGridContactorsSet());

		sut //
				.withMainContactor(false) //
				.withGroundingContactor(true);
		assertFalse(context.isOnGridContactorsSet());

		sut //
				.withMainContactor(true) //
				.withGroundingContactor(true);
		assertFalse(context.isOnGridContactorsSet());
	}

	@Test
	public void testIsOffGridContactorsSet() {
		var sut = new DummyOffGridSwitch("offGridSwitch0");
		var context = new Context(null, null, null, sut, null, null);

		sut //
				.withMainContactor(false) //
				.withGroundingContactor(false);
		assertFalse(context.isOffGridContactorsSet());

		sut //
				.withMainContactor(true) //
				.withGroundingContactor(false);
		assertFalse(context.isOffGridContactorsSet());

		sut //
				.withMainContactor(false) //
				.withGroundingContactor(true);
		assertFalse(context.isOffGridContactorsSet());

		sut //
				.withMainContactor(true) //
				.withGroundingContactor(true);
		assertTrue(context.isOffGridContactorsSet());
	}

}
