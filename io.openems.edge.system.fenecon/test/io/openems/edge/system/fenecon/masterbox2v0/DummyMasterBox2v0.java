package io.openems.edge.system.fenecon.masterbox2v0;

import static io.openems.edge.common.test.TestUtils.withValue;

import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;

public class DummyMasterBox2v0 extends AbstractDummyOpenemsComponent<DummyMasterBox2v0> implements MasterBox2v0 {

	public DummyMasterBox2v0(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				MasterBox2v0.ChannelId.values() //
		);
	}

	@Override
	protected DummyMasterBox2v0 self() {
		return this;
	}

	/**
	 * Set {@link MasterBox2v0.ChannelId#RELAY_1}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyMasterBox2v0 withRelay1(boolean value) {
		withValue(this, MasterBox2v0.ChannelId.RELAY_1, value);
		return this.self();
	}

	/**
	 * Set {@link MasterBox2v0.ChannelId#RELAY_2}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyMasterBox2v0 withRelay2(boolean value) {
		withValue(this, MasterBox2v0.ChannelId.RELAY_2, value);
		return this.self();
	}

	/**
	 * Set {@link MasterBox2v0.ChannelId#RELAY_3}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyMasterBox2v0 withRelay3(boolean value) {
		withValue(this, MasterBox2v0.ChannelId.RELAY_3, value);
		return this.self();
	}

	/**
	 * Set {@link MasterBox2v0.ChannelId#RELAY_4}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyMasterBox2v0 withRelay4(boolean value) {
		withValue(this, MasterBox2v0.ChannelId.RELAY_4, value);
		return this.self();
	}

	/**
	 * Set {@link MasterBox2v0.ChannelId#RELAY_5}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyMasterBox2v0 withRelay5(boolean value) {
		withValue(this, MasterBox2v0.ChannelId.RELAY_5, value);
		return this.self();
	}

	/**
	 * Set {@link MasterBox2v0.ChannelId#RELAY_6}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyMasterBox2v0 withRelay6(boolean value) {
		withValue(this, MasterBox2v0.ChannelId.RELAY_6, value);
		return this.self();
	}

	/**
	 * Set {@link MasterBox2v0.ChannelId#ANALOG_OUT_VOLTAGE}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyMasterBox2v0 withAnalogOutVoltage(int value) {
		withValue(this, MasterBox2v0.ChannelId.ANALOG_OUT_VOLTAGE, value);
		return this.self();
	}

	/**
	 * Set {@link MasterBox2v0.ChannelId#ANALOG_OUT_CONTROL}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyMasterBox2v0 withAnalogOutControl(boolean value) {
		withValue(this, MasterBox2v0.ChannelId.ANALOG_OUT_CONTROL, value);
		return this.self();
	}

	/**
	 * Set {@link MasterBox2v0.ChannelId#VOLTAGE_L1_ENERGYMETER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyMasterBox2v0 withVoltageL1EnergyMeter(int value) {
		withValue(this, MasterBox2v0.ChannelId.VOLTAGE_L1_ENERGYMETER, value);
		return this.self();
	}

	/**
	 * Set {@link MasterBox2v0.ChannelId#VOLTAGE_L2_ENERGYMETER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyMasterBox2v0 withVoltageL2EnergyMeter(int value) {
		withValue(this, MasterBox2v0.ChannelId.VOLTAGE_L2_ENERGYMETER, value);
		return this.self();
	}

	/**
	 * Set {@link MasterBox2v0.ChannelId#VOLTAGE_L3_ENERGYMETER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyMasterBox2v0 withVoltageL3EnergyMeter(int value) {
		withValue(this, MasterBox2v0.ChannelId.VOLTAGE_L3_ENERGYMETER, value);
		return this.self();
	}

	/**
	 * Set {@link MasterBox2v0.ChannelId#CURRENT_L1_ENERGYMETER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyMasterBox2v0 withCurrentL1EnergyMeter(int value) {
		withValue(this, MasterBox2v0.ChannelId.CURRENT_L1_ENERGYMETER, value);
		return this.self();
	}

	/**
	 * Set {@link MasterBox2v0.ChannelId#CURRENT_L2_ENERGYMETER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyMasterBox2v0 withCurrentL2EnergyMeter(int value) {
		withValue(this, MasterBox2v0.ChannelId.CURRENT_L2_ENERGYMETER, value);
		return this.self();
	}

	/**
	 * Set {@link MasterBox2v0.ChannelId#CURRENT_L3_ENERGYMETER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyMasterBox2v0 withCurrentL3EnergyMeter(int value) {
		withValue(this, MasterBox2v0.ChannelId.CURRENT_L3_ENERGYMETER, value);
		return this.self();
	}

	/**
	 * Set {@link MasterBox2v0.ChannelId#ACTIVE_POWER_L1_ENERGYMETER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyMasterBox2v0 withActivePowerL1EnergyMeter(int value) {
		withValue(this, MasterBox2v0.ChannelId.ACTIVE_POWER_L1_ENERGYMETER, value);
		return this.self();
	}

	/**
	 * Set {@link MasterBox2v0.ChannelId#ACTIVE_POWER_L2_ENERGYMETER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyMasterBox2v0 withActivePowerL2EnergyMeter(int value) {
		withValue(this, MasterBox2v0.ChannelId.ACTIVE_POWER_L2_ENERGYMETER, value);
		return this.self();
	}

	/**
	 * Set {@link MasterBox2v0.ChannelId#ACTIVE_POWER_L3_ENERGYMETER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyMasterBox2v0 withActivePowerL3EnergyMeter(int value) {
		withValue(this, MasterBox2v0.ChannelId.ACTIVE_POWER_L3_ENERGYMETER, value);
		return this.self();
	}

	/**
	 * Set {@link MasterBox2v0.ChannelId#TIME_STAMP_ENERGYMETER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyMasterBox2v0 withTimeStampEnergyMeter(long value) {
		withValue(this, MasterBox2v0.ChannelId.TIME_STAMP_ENERGYMETER, value);
		return this.self();
	}

	/**
	 * Set {@link MasterBox2v0.ChannelId#STATUS_ENERGYMETER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyMasterBox2v0 withStatusEnergyMeter(int value) {
		withValue(this, MasterBox2v0.ChannelId.STATUS_ENERGYMETER, value);
		return this.self();
	}
}
