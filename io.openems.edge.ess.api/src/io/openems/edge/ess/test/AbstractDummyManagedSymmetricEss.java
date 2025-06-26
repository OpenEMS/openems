package io.openems.edge.ess.test;

import java.util.function.Consumer;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

public abstract class AbstractDummyManagedSymmetricEss<SELF extends AbstractDummySymmetricEss<?>> extends
		AbstractDummySymmetricEss<SELF> implements ManagedSymmetricEss, SymmetricEss, StartStoppable, OpenemsComponent {

	public static final int MAX_APPARENT_POWER = Integer.MAX_VALUE;

	private Power power = new DummyPower();

	private int powerPrecision = 1;
	private Consumer<SymmetricApplyPowerRecord> symmetricApplyPowerCallback = null;

	protected AbstractDummyManagedSymmetricEss(String id,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(id, firstInitialChannelIds, furtherInitialChannelIds);
	}

	/**
	 * Sets the {@link Power} implementation.
	 * 
	 * @param power the power
	 * @return myself
	 */
	public final SELF setPower(Power power) {
		this.power = power;
		return this.self();
	}

	@Override
	public final Power getPower() {
		return this.power;
	}

	@Override
	public final int getPowerPrecision() {
		return this.powerPrecision;
	}

	/**
	 * Set {@link SymmetricEss.ChannelId#MAX_APPARENT_POWER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withMaxApparentPower(int value) {
		super.withMaxApparentPower(value);
		if (this.power instanceof DummyPower p) {
			p.setMaxApparentPower(value);
		}
		return this.self();
	}

	/**
	 * Set {@link ManagedSymmetricEss.ChannelId#ALLOWED_CHARGE_POWER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withAllowedChargePower(int value) {
		TestUtils.withValue(this, ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, value);
		return this.self();
	}

	/**
	 * Set {@link ManagedSymmetricEss.ChannelId#ALLOWED_DISCHARGE_POWER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withAllowedDischargePower(int value) {
		TestUtils.withValue(this, ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, value);
		return this.self();
	}

	/**
	 * Set Power Precision.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withPowerPrecision(int value) {
		this.powerPrecision = value;
		return this.self();
	}

	/**
	 * Set callback for applyPower().
	 *
	 * @param callback the callback
	 * @return myself
	 */
	public final SELF withSymmetricApplyPowerCallback(Consumer<SymmetricApplyPowerRecord> callback) {
		this.symmetricApplyPowerCallback = callback;
		return this.self();
	}

	@Override
	public final void applyPower(int activePower, int reactivePower) {
		if (this.symmetricApplyPowerCallback != null) {
			this.symmetricApplyPowerCallback.accept(new SymmetricApplyPowerRecord(activePower, reactivePower));
		}
	}

	public record SymmetricApplyPowerRecord(int activePower, int reactivePower) {
	}

	@Override
	public final void setStartStop(StartStop value) throws OpenemsNamedException {
		this._setStartStop(value);
	}
}
