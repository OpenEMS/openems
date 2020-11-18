package io.openems.edge.ess.test;

import java.util.function.Consumer;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

public abstract class AbstractDummyManagedSymmetricEss<T extends AbstractDummyManagedSymmetricEss<?>>
		extends AbstractOpenemsComponent implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent {

	public static final int MAX_APPARENT_POWER = Integer.MAX_VALUE;

	private final Power power;

	private int powerPrecision = 1;
	private Consumer<SymmetricApplyPowerRecord> symmetricApplyPowerCallback = null;

	protected AbstractDummyManagedSymmetricEss(String id, Power power,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		this.power = power;
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}

	protected abstract T self();

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
	 * {@link AbstractDummyManagedSymmetricEss}.
	 * 
	 * @param value the state-of-charge
	 * @return myself
	 */
	public T withSoc(int value) {
		this._setSoc(value);
		this.getSocChannel().nextProcessImage();
		return this.self();
	}

	/**
	 * Set {@link SymmetricEss.ChannelId#CAPACITY} of this
	 * {@link AbstractDummyManagedSymmetricEss}. *
	 * 
	 * @param value the capacity
	 * @return myself
	 */
	public T withCapacity(int value) {
		this._setCapacity(value);
		this.getCapacityChannel().nextProcessImage();
		return this.self();
	}

	/**
	 * Set {@link SymmetricEss.ChannelId#GRID_MODE} of this
	 * {@link AbstractDummyManagedSymmetricEss}. *
	 * 
	 * @param value the {@link GridMode}
	 * @return myself
	 */
	public T withGridMode(GridMode value) {
		this._setGridMode(value);
		this.getGridModeChannel().nextProcessImage();
		return this.self();
	}

	/**
	 * Set {@link SymmetricEss.ChannelId#MAX_APPARENT_POWER} of this
	 * {@link AbstractDummyManagedSymmetricEss}. *
	 * 
	 * @param value the max apparent power
	 * @return myself
	 */
	public T withMaxApparentPower(int value) {
		this._setMaxApparentPower(value);
		this.getMaxApparentPowerChannel().nextProcessImage();
		return this.self();
	}

	/**
	 * Set {@link ManagedSymmetricEss.ChannelId#ALLOWED_CHARGE_POWER} of this
	 * {@link AbstractDummyManagedSymmetricEss}. *
	 * 
	 * @param value the allowed charge power
	 * @return myself
	 */
	public T withAllowedChargePower(int value) {
		this._setAllowedChargePower(value);
		this.getAllowedChargePowerChannel().nextProcessImage();
		return this.self();
	}

	/**
	 * Set {@link ManagedSymmetricEss.ChannelId#ALLOWED_DISCHARGE_POWER} of this
	 * {@link AbstractDummyManagedSymmetricEss}. *
	 * 
	 * @param value the allowed discharge power
	 * @return myself
	 */
	public T withAllowedDischargePower(int value) {
		this._setAllowedDischargePower(value);
		this.getAllowedDischargePowerChannel().nextProcessImage();
		return this.self();
	}

	/**
	 * Set Power Precision of this {@link AbstractDummyManagedSymmetricEss}.
	 * 
	 * @param value the power precision
	 * @return myself
	 */
	public T withPowerPrecision(int value) {
		this.powerPrecision = value;
		return this.self();
	}

	/**
	 * Set callback for applyPower() of this
	 * {@link AbstractDummyManagedSymmetricEss}.
	 * 
	 * @param callback the callback
	 * @return myself
	 */
	public T withSymmetricApplyPowerCallback(Consumer<SymmetricApplyPowerRecord> callback) {
		this.symmetricApplyPowerCallback = callback;
		return this.self();
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
