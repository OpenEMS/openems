package io.openems.edge.batteryinverter.sinexcel.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.sinexcel.Config;
import io.openems.edge.batteryinverter.sinexcel.Sinexcel;
import io.openems.edge.batteryinverter.sinexcel.enums.FalseTrue;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<Sinexcel> {

	protected final Config config;
	protected final int setActivePower;
	protected final int setReactivePower;

	public Context(Sinexcel parent, Config config, int setActivePower, int setReactivePower) {
		super(parent);
		this.config = config;
		this.setActivePower = setActivePower;
		this.setReactivePower = setReactivePower;
	}

	/**
	 * Starts the inverter.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	protected void setInverterOn() throws OpenemsNamedException {
		EnumWriteChannel setdataModOnCmd = this.getParent().channel(Sinexcel.ChannelId.MOD_ON_CMD);
		setdataModOnCmd.setNextWriteValue(FalseTrue.TRUE); // true = START
	}

	/**
	 * Stops the inverter.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	protected void setInverterOff() throws OpenemsNamedException {
		EnumWriteChannel setdataModOffCmd = this.getParent().channel(Sinexcel.ChannelId.MOD_OFF_CMD);
		setdataModOffCmd.setNextWriteValue(FalseTrue.TRUE); // true = STOP
	}

	/**
	 * Executes a Soft-Start. Sets the internal DC relay. Once this register is set
	 * to 1, the PCS will start the soft-start procedure, otherwise, the PCS will do
	 * nothing on the DC input Every time the PCS is powered off, this register will
	 * be cleared to 0. In some particular cases, the BMS wants to re-softstart, the
	 * EMS should actively clear this register to 0, after BMS soft-started, set it
	 * to 1 again.
	 *
	 * @throws OpenemsNamedException on error
	 */
	protected void softStart(boolean switchOn) throws OpenemsNamedException {
		IntegerWriteChannel setDcRelay = this.getParent().channel(Sinexcel.ChannelId.SET_INTERN_DC_RELAY);
		setDcRelay.setNextWriteValue(switchOn ? 1 : 0);
	}

	/**
	 * At first the PCS needs a stop command, then is required to remove the AC
	 * connection, after that the Grid OFF command.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	public void islandOn() throws OpenemsNamedException {
		IntegerWriteChannel setAntiIslanding = this.getParent().channel(Sinexcel.ChannelId.SET_ANTI_ISLANDING);
		setAntiIslanding.setNextWriteValue(0); // Disabled
		IntegerWriteChannel setDataGridOffCmd = this.getParent().channel(Sinexcel.ChannelId.OFF_GRID_CMD);
		setDataGridOffCmd.setNextWriteValue(1); // Stop
	}

//	/**
//	 * At first the PCS needs a stop command, then is required to plug in the AC
//	 * connection, after that the Grid ON command.
//	 * 
//	 * @throws OpenemsNamedException on error
//	 */
//	public void islandingOff() throws OpenemsNamedException {
//		IntegerWriteChannel setAntiIslanding = this.channel(Sinexcel.ChannelId.SET_ANTI_ISLANDING);
//		setAntiIslanding.setNextWriteValue(1); // Enabled
//		IntegerWriteChannel setdataGridOnCmd = this.channel(Sinexcel.ChannelId.OFF_GRID_CMD);
//		setdataGridOnCmd.setNextWriteValue(1); // Start
//	}
//
//	public void doHandlingSlowFloatVoltage() throws OpenemsNamedException {
//		// System.out.println("Upper voltage : " +
//		// this.channel(EssSinexcel.ChannelId.UPPER_VOLTAGE_LIMIT).value().asStringWithoutUnit());
//		IntegerWriteChannel setSlowChargeVoltage = this.channel(Sinexcel.ChannelId.SET_SLOW_CHARGE_VOLTAGE);
//		setSlowChargeVoltage.setNextWriteValue(this.slowChargeVoltage);
//		IntegerWriteChannel setFloatChargeVoltage = this.channel(Sinexcel.ChannelId.SET_FLOAT_CHARGE_VOLTAGE);
//		setFloatChargeVoltage.setNextWriteValue(this.floatChargeVoltage);
//	}
//
//	public boolean faultIslanding() {
//		StateChannel i = this.channel(Sinexcel.ChannelId.STATE_4);
//		Optional<Boolean> islanding = i.getNextValue().asOptional();
//		return islanding.isPresent() && islanding.get();
//	}
//	/**
//	 * Is Grid Shutdown?.
//	 * 
//	 * @return true if grid is shut down
//	 */
//	public boolean faultIslanding() {
//		StateChannel channel = this.channel(EssSinexcel.ChannelId.STATE_4);
//		Optional<Boolean> islanding = channel.getNextValue().asOptional();
//		return islanding.isPresent() && islanding.get();
//	}
//
//	/**
//	 * Is inverter state ON?.
//	 * 
//	 * @return true if inverter is in ON-State
//	 */
//	public boolean isStateOn() {
//		StateChannel channel = this.channel(EssSinexcel.ChannelId.STATE_18);
//		Optional<Boolean> stateOff = channel.getNextValue().asOptional();
//		return stateOff.isPresent() && stateOff.get();
//	}

	// SF: was commented before
//	public boolean stateOn() {
//		StateChannel v = this.channel(EssSinexcel.ChannelId.Sinexcel_STATE_9);
//		Optional<Boolean> stateOff = v.getNextValue().asOptional(); 
//		return stateOff.isPresent() && stateOff.get();
//	}

}