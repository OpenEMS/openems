package io.openems.edge.ess.test;

import java.util.function.Consumer;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

/**
 * Provides a simple, simulated ManagedSymmetricEss component that can be used
 * together with the OpenEMS Component test framework.
 */
public class DummyManagedSymmetricEss extends AbstractOpenemsComponent
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent {

	public static final int MAX_APPARENT_POWER = Integer.MAX_VALUE;

	private final Power power;

	private int powerPrecision = 1;
	private Consumer<SymmetricApplyPowerRecord> symmetricApplyPowerCallback = null;

	protected DummyManagedSymmetricEss(String id, Power power,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		this.power = power;
		if (power instanceof DummyPower) {
			((DummyPower) power).addEss(this);
		}
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}

	public DummyManagedSymmetricEss(String id, Power power) {
		this(id, power, //
				OpenemsComponent.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				SymmetricEss.ChannelId.values() //
		);
	}

	public DummyManagedSymmetricEss(String id) {
		this(id, new DummyPower(MAX_APPARENT_POWER));
	}

	public DummyManagedSymmetricEss(String id, int maxApparentPower) {
		this(id, new DummyPower(maxApparentPower));
	}

	public DummyManagedSymmetricEss setMaxApparentPower(Integer value) {
		this._setMaxApparentPower(value);
		this.getMaxApparentPowerChannel().nextProcessImage();
		return this;
	}

	public DummyManagedSymmetricEss setGridMode(GridMode gridMode) {
		this._setGridMode(gridMode);
		this.getGridModeChannel().nextProcessImage();
		return this;
	}

	public DummyManagedSymmetricEss setSoc(int soc) {
		this._setSoc(soc);
		this.getSocChannel().nextProcessImage();
		return this;
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		return this.powerPrecision;
	}

	/**
	 * Set {@link SymmetricEss.ChannelId#SOC} of this
	 * {@link DummyManagedSymmetricEss}.
	 *
	 * @param value the state-of-charge
	 * @return myself
	 */
	public DummyManagedSymmetricEss withSoc(int value) {
		this._setSoc(value);
		this.getSocChannel().nextProcessImage();
		return this;
	}

	/**
	 * Set {@link SymmetricEss.ChannelId#CAPACITY} of this
	 * {@link DummyManagedSymmetricEss}. *
	 *
	 * @param value the capacity
	 * @return myself
	 */
	public DummyManagedSymmetricEss withCapacity(int value) {
		this._setCapacity(value);
		this.getCapacityChannel().nextProcessImage();
		return this;
	}

	/**
	 * Set {@link SymmetricEss.ChannelId#GRID_MODE} of this
	 * {@link DummyManagedSymmetricEss}. *
	 *
	 * @param value the {@link GridMode}
	 * @return myself
	 */
	public DummyManagedSymmetricEss withGridMode(GridMode value) {
		this._setGridMode(value);
		this.getGridModeChannel().nextProcessImage();
		return this;
	}

	/**
	 * Set {@link SymmetricEss.ChannelId#MAX_APPARENT_POWER} of this
	 * {@link DummyManagedSymmetricEss}. *
	 *
	 * @param value the max apparent power
	 * @return myself
	 */
	public DummyManagedSymmetricEss withMaxApparentPower(int value) {
		this._setMaxApparentPower(value);
		this.getMaxApparentPowerChannel().nextProcessImage();
		return this;
	}

	/**
	 * Set {@link ManagedSymmetricEss.ChannelId#ALLOWED_CHARGE_POWER} of this
	 * {@link DummyManagedSymmetricEss}. *
	 *
	 * @param value the allowed charge power
	 * @return myself
	 */
	public DummyManagedSymmetricEss withAllowedChargePower(int value) {
		this._setAllowedChargePower(value);
		this.getAllowedChargePowerChannel().nextProcessImage();
		return this;
	}

	/**
	 * Set {@link ManagedSymmetricEss.ChannelId#ALLOWED_DISCHARGE_POWER} of this
	 * {@link DummyManagedSymmetricEss}. *
	 *
	 * @param value the allowed discharge power
	 * @return myself
	 */
	public DummyManagedSymmetricEss withAllowedDischargePower(int value) {
		this._setAllowedDischargePower(value);
		this.getAllowedDischargePowerChannel().nextProcessImage();
		return this;
	}

	/**
	 * Set Power Precision of this {@link DummyManagedSymmetricEss}.
	 *
	 * @param value the power precision
	 * @return myself
	 */
	public DummyManagedSymmetricEss withPowerPrecision(int value) {
		this.powerPrecision = value;
		return this;
	}

	/**
	 * Set callback for applyPower() of this {@link DummyManagedSymmetricEss}.
	 *
	 * @param callback the callback
	 * @return myself
	 */
	public DummyManagedSymmetricEss withSymmetricApplyPowerCallback(Consumer<SymmetricApplyPowerRecord> callback) {
		this.symmetricApplyPowerCallback = callback;
		return this;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
		if (this.symmetricApplyPowerCallback != null) {
			this.symmetricApplyPowerCallback.accept(new SymmetricApplyPowerRecord(activePower, reactivePower));
		}
	}

	public static class SymmetricApplyPowerRecord {
		public final int activePower;
		public final int reactivePower;

		public SymmetricApplyPowerRecord(int activePower, int reactivePower) {
			this.activePower = activePower;
			this.reactivePower = reactivePower;
		}
	}
}
