package io.openems.edge.battery.test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.common.test.TestUtils;

public abstract class AbstractDummyBattery<SELF extends AbstractDummyBattery<?>>
		extends AbstractDummyOpenemsComponent<SELF> implements Battery, StartStoppable {

	protected AbstractDummyBattery(String id, io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(id, firstInitialChannelIds, furtherInitialChannelIds);
	}

	@Override
	public final void setStartStop(StartStop value) throws OpenemsNamedException {
		this.withStartStop(value);
	}

	/**
	 * Set {@link StartStoppable.ChannelId#START_STOP}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withStartStop(StartStop value) {
		TestUtils.withValue(this, StartStoppable.ChannelId.START_STOP, value);
		return this.self();
	}

	/**
	 * Set {@link Battery.ChannelId#SOC}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withSoc(int value) {
		TestUtils.withValue(this, Battery.ChannelId.SOC, value);
		return this.self();
	}

	/**
	 * Set {@link Battery.ChannelId#SOH}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withSoh(int value) {
		TestUtils.withValue(this, Battery.ChannelId.SOH, value);
		return this.self();
	}

	/**
	 * Set {@link Battery.ChannelId#CAPACITY}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withCapacity(int value) {
		TestUtils.withValue(this, Battery.ChannelId.CAPACITY, value);
		return this.self();
	}

	/**
	 * Set {@link Battery.ChannelId#VOLTAGE}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withVoltage(int value) {
		TestUtils.withValue(this, Battery.ChannelId.VOLTAGE, value);
		return this.self();
	}

	/**
	 * Set {@link Battery.ChannelId#CURRENT}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withCurrent(int value) {
		TestUtils.withValue(this, Battery.ChannelId.CURRENT, value);
		return this.self();
	}

	/**
	 * Set {@link Battery.ChannelId#DISCHARGE_MAX_CURRENT}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withDischargeMaxCurrent(int value) {
		TestUtils.withValue(this, Battery.ChannelId.DISCHARGE_MAX_CURRENT, value);
		return this.self();
	}

	/**
	 * Set {@link Battery.ChannelId#CHARGE_MAX_CURRENT}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withChargeMaxCurrent(int value) {
		TestUtils.withValue(this, Battery.ChannelId.CHARGE_MAX_CURRENT, value);
		return this.self();
	}

	/**
	 * Set {@link Battery.ChannelId#DISCHARGE_MIN_VOLTAGE}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withDischargeMinVoltage(int value) {
		TestUtils.withValue(this, Battery.ChannelId.DISCHARGE_MIN_VOLTAGE, value);
		return this.self();
	}

	/**
	 * Set {@link Battery.ChannelId#CHARGE_MAX_VOLTAGE}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withChargeMaxVoltage(int value) {
		TestUtils.withValue(this, Battery.ChannelId.CHARGE_MAX_VOLTAGE, value);
		return this.self();
	}

	/**
	 * Set {@link Battery.ChannelId#MIN_CELL_VOLTAGE}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withMinCellVoltage(int value) {
		TestUtils.withValue(this, Battery.ChannelId.MIN_CELL_VOLTAGE, value);
		return this.self();
	}

	/**
	 * Set {@link Battery.ChannelId#MAX_CELL_VOLTAGE}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withMaxCellVoltage(int value) {
		TestUtils.withValue(this, Battery.ChannelId.MAX_CELL_VOLTAGE, value);
		return this.self();
	}

	/**
	 * Set {@link Battery.ChannelId#MIN_CELL_TEMPERATURE}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withMinCellTemperature(int value) {
		TestUtils.withValue(this, Battery.ChannelId.MIN_CELL_TEMPERATURE, value);
		return this.self();
	}

	/**
	 * Set {@link Battery.ChannelId#MAX_CELL_TEMPERATURE}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withMaxCellTemperature(int value) {
		TestUtils.withValue(this, Battery.ChannelId.MAX_CELL_TEMPERATURE, value);
		return this.self();
	}

	/**
	 * Set {@link Battery.ChannelId#INNER_RESISTANCE}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withInnerResistence(int value) {
		TestUtils.withValue(this, Battery.ChannelId.INNER_RESISTANCE, value);
		return this.self();
	}

}
