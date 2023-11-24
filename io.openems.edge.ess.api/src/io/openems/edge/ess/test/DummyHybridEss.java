package io.openems.edge.ess.test;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

/**
 * Provides a simple, simulated {@link HybridEss} that is also a
 * {@link ManagedSymmetricEss} component and can be used together with the
 * OpenEMS Component test framework.
 */
public class DummyHybridEss extends AbstractDummyManagedSymmetricEss<DummyHybridEss>
		implements HybridEss, ManagedSymmetricEss, SymmetricEss, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SURPLUS_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
		);

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}

	}

	public DummyHybridEss(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				HybridEss.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Override
	protected DummyHybridEss self() {
		return this;
	}

	/**
	 * Set {@link HybridEss.ChannelId#DC_DISCHARGE_POWER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final DummyHybridEss withDcDischargePower(Integer value) {
		TestUtils.withValue(this, HybridEss.ChannelId.DC_DISCHARGE_POWER, value);
		return this;
	}

	/**
	 * Set {@link ChannelId#SURPLUS_POWER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final DummyHybridEss withSurplusPower(Integer value) {
		TestUtils.withValue(this, ChannelId.SURPLUS_POWER, value);
		return this;
	}

	/**
	 * Gets the Channel for {@link ChannelId#SURPLUS_POWER}.
	 *
	 * @return the Channel
	 */
	private final IntegerReadChannel getSurplusPowerChannel() {
		return this.channel(ChannelId.SURPLUS_POWER);
	}

	/**
	 * Gets the Dummy Surplus Power in [W]. See {@link ChannelId#SURPLUS_POWER}.
	 *
	 * @return the Channel {@link Value} or null
	 */
	@Override
	public final Integer getSurplusPower() {
		return this.getSurplusPowerChannel().value().get();
	}

}
