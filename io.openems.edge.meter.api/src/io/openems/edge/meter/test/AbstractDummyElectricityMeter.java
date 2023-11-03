package io.openems.edge.meter.test;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;

public abstract class AbstractDummyElectricityMeter<SELF extends AbstractDummyElectricityMeter<?>>
		extends AbstractOpenemsComponent implements ElectricityMeter {

	private MeterType meterType;

	protected AbstractDummyElectricityMeter(String id,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}

	protected abstract SELF self();

	/**
	 * Set the {@link MeterType}.
	 *
	 * @param meterType the meterType
	 * @return myself
	 */
	public SELF withMeterType(MeterType meterType) {
		this.meterType = meterType;
		return this.self();
	}

	/**
	 * Set {@link ElectricityMeter.ChannelId#ACTIVE_POWER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withActivePower(Integer value) {
		TestUtils.withValue(this, ElectricityMeter.ChannelId.ACTIVE_POWER, value);
		return this.self();
	}

	/**
	 * Set {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L1}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withActivePowerL1(Integer value) {
		TestUtils.withValue(this, ElectricityMeter.ChannelId.ACTIVE_POWER_L1, value);
		return this.self();
	}

	/**
	 * Set {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L2}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withActivePowerL2(Integer value) {
		TestUtils.withValue(this, ElectricityMeter.ChannelId.ACTIVE_POWER_L2, value);
		return this.self();
	}

	/**
	 * Set {@link ElectricityMeter.ChannelId#ACTIVE_POWER_L3}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withActivePowerL3(Integer value) {
		TestUtils.withValue(this, ElectricityMeter.ChannelId.ACTIVE_POWER_L3, value);
		return this.self();
	}

	/**
	 * Set {@link ElectricityMeter.ChannelId#REACTIVE_POWER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withReactivePower(Integer value) {
		TestUtils.withValue(this, ElectricityMeter.ChannelId.REACTIVE_POWER, value);
		return this.self();
	}

	/**
	 * Set {@link ElectricityMeter.ChannelId#REACTIVE_POWER_L1}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withReactivePowerL1(Integer value) {
		TestUtils.withValue(this, ElectricityMeter.ChannelId.REACTIVE_POWER_L1, value);
		return this.self();
	}

	/**
	 * Set {@link ElectricityMeter.ChannelId#REACTIVE_POWER_L2}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withReactivePowerL2(Integer value) {
		TestUtils.withValue(this, ElectricityMeter.ChannelId.REACTIVE_POWER_L2, value);
		return this.self();
	}

	/**
	 * Set {@link ElectricityMeter.ChannelId#REACTIVE_POWER_L3}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withReactivePowerL3(Integer value) {
		TestUtils.withValue(this, ElectricityMeter.ChannelId.REACTIVE_POWER_L3, value);
		return this.self();
	}

	/**
	 * Set {@link ElectricityMeter.ChannelId#CURRENT}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withCurrent(Integer value) {
		TestUtils.withValue(this, ElectricityMeter.ChannelId.CURRENT, value);
		return this.self();
	}

	/**
	 * Set {@link ElectricityMeter.ChannelId#CURRENT_L1}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withCurrentL1(Integer value) {
		TestUtils.withValue(this, ElectricityMeter.ChannelId.CURRENT_L1, value);
		return this.self();
	}

	/**
	 * Set {@link ElectricityMeter.ChannelId#CURRENT_L2}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withCurrentL2(Integer value) {
		TestUtils.withValue(this, ElectricityMeter.ChannelId.CURRENT_L2, value);
		return this.self();
	}

	/**
	 * Set {@link ElectricityMeter.ChannelId#CURRENT_L3}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withCurrentL3(Integer value) {
		TestUtils.withValue(this, ElectricityMeter.ChannelId.CURRENT_L3, value);
		return this.self();
	}

	/**
	 * Set {@link ElectricityMeter.ChannelId#VOLTAGE}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withVoltage(Integer value) {
		TestUtils.withValue(this, ElectricityMeter.ChannelId.VOLTAGE, value);
		return this.self();
	}

	/**
	 * Set {@link ElectricityMeter.ChannelId#VOLTAGE_L1}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withVoltageL1(Integer value) {
		TestUtils.withValue(this, ElectricityMeter.ChannelId.VOLTAGE_L1, value);
		return this.self();
	}

	/**
	 * Set {@link ElectricityMeter.ChannelId#VOLTAGE_L2}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withVoltageL2(Integer value) {
		TestUtils.withValue(this, ElectricityMeter.ChannelId.VOLTAGE_L2, value);
		return this.self();
	}

	/**
	 * Set {@link ElectricityMeter.ChannelId#VOLTAGE_L3}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withVoltageL3(Integer value) {
		TestUtils.withValue(this, ElectricityMeter.ChannelId.VOLTAGE_L3, value);
		return this.self();
	}

	/**
	 * Set {@link ElectricityMeter.ChannelId#ACTIVE_PRODUCTION_ENERGY}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withActiveProductionEnergy(Integer value) {
		TestUtils.withValue(this, ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, value);
		return this.self();
	}

	/**
	 * Set {@link ElectricityMeter.ChannelId#ACTIVE_PRODUCTION_ENERGY_L1}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withActiveProductionEnergyL1(Integer value) {
		TestUtils.withValue(this, ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, value);
		return this.self();
	}

	/**
	 * Set {@link ElectricityMeter.ChannelId#ACTIVE_PRODUCTION_ENERGY_L2}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withActiveProductionEnergyL2(Integer value) {
		TestUtils.withValue(this, ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, value);
		return this.self();
	}

	/**
	 * Set {@link ElectricityMeter.ChannelId#ACTIVE_PRODUCTION_ENERGY_L3}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withActiveProductionEnergyL3(Integer value) {
		TestUtils.withValue(this, ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, value);
		return this.self();
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

}
