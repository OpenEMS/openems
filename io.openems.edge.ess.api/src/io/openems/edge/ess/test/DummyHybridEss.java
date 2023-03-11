package io.openems.edge.ess.test;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

/**
 * Provides a simple, simulated {@link HybridEss} that is also a
 * {@link ManagedSymmetricEss} component and can be used together with the
 * OpenEMS Component test framework.
 */
public class DummyHybridEss extends AbstractOpenemsComponent
		implements HybridEss, ManagedSymmetricEss, SymmetricEss, OpenemsComponent {

	public static final int MAX_APPARENT_POWER = Integer.MAX_VALUE;

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

	private final Power power;

	public DummyHybridEss(String id, Power power) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				HybridEss.ChannelId.values(), //
				ChannelId.values() //
		);
		this.power = power;
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}

	public DummyHybridEss(String id) {
		this(id, new DummyPower(MAX_APPARENT_POWER));
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	/**
	 * Set {@link SymmetricEss.ChannelId#ACTIVE_POWER} of this
	 * {@link DummyHybridEss}.
	 *
	 * @param value the active power
	 * @return myself
	 */
	public DummyHybridEss withActivePower(Integer value) {
		this._setActivePower(value);
		this.getActivePowerChannel().nextProcessImage();
		return this;
	}

	/**
	 * Set {@link HybridEss.ChannelId#DC_DISCHARGE_POWER} of this
	 * {@link DummyHybridEss}.
	 *
	 * @param value the DC discharge power
	 * @return myself
	 */
	public DummyHybridEss withDcDischargePower(Integer value) {
		this._setDcDischargePower(value);
		this.getDcDischargePowerChannel().nextProcessImage();
		return this;
	}

	/**
	 * Set {@link ChannelId#SURPLUS_POWER} of this {@link DummyHybridEss}.
	 *
	 * @param value the surplus power
	 * @return myself
	 */
	public DummyHybridEss withSurplusPower(Integer value) {
		this._setSurplusPower(value);
		this.getSurplusPowerChannel().nextProcessImage();
		return this;
	}

	/**
	 * Set {@link SymmetricEss.ChannelId#MAX_APPARENT_POWER} of this
	 * {@link DummyHybridEss}.
	 *
	 * @param value the max apparent power
	 * @return myself
	 */
	public DummyHybridEss withMaxApparentPower(int value) {
		this._setMaxApparentPower(value);
		this.getMaxApparentPowerChannel().nextProcessImage();
		if (this.power instanceof DummyPower) {
			((DummyPower) this.power).setMaxApparentPower(value);
		}
		return this;
	}

	/**
	 * Gets the Channel for {@link ChannelId#SURPLUS_POWER}.
	 *
	 * @return the Channel
	 */
	private IntegerReadChannel getSurplusPowerChannel() {
		return this.channel(ChannelId.SURPLUS_POWER);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SURPLUS_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	private void _setSurplusPower(Integer value) {
		this.getSurplusPowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Dummy Surplus Power in [W]. See {@link ChannelId#SURPLUS_POWER}.
	 *
	 * @return the Channel {@link Value} or null
	 */
	@Override
	public Integer getSurplusPower() {
		return this.getSurplusPowerChannel().value().get();
	}
}
