package io.openems.edge.evse.chargepoint.keba.udp;

import io.openems.common.channel.Debounce;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.meter.api.ElectricityMeter;

public interface EvseChargePointKebaUdp extends EvseChargePoint, ElectricityMeter, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		COMMUNICATION_FAILED(Doc.of(Level.FAULT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Communication to wallbox Failed " //
						+ "| Keine Verbindung zur Ladestation " //
						+ "| Bitte überprüfen Sie die Kommunikationsverbindung zu der Ladestation")), //

		// copied from evcs keba

		ERROR_1(Doc.of(OpenemsType.INTEGER) //
				.text("Detail code for state ERROR; exceptions see FAQ on www.kecontact.com")), //
		ERROR_2(Doc.of(OpenemsType.INTEGER) //
				.text("Detail code for state ERROR; exceptions see FAQ on www.kecontact.com")), //
		DIP_SWITCH_1(Doc.of(OpenemsType.STRING) //
				.text("The first eight dip switch settings as binary")),
		DIP_SWITCH_2(Doc.of(OpenemsType.STRING) //
				.text("The second eight dip switch settings as binary")),
		DIP_SWITCH_MAX_HW(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.text("The raw maximum limit configured by the dip switches")),
		PRODUCT(Doc.of(OpenemsType.STRING) //
				.text("Model name (variant)")), //
		SERIAL(Doc.of(OpenemsType.STRING)), //
		COM_MODULE(Doc.of(OpenemsType.STRING) //
				.text("Communication module is installed; KeContact P30 only")),
		ENABLE_SYS(Doc.of(OpenemsType.BOOLEAN) //
				.text("Enable state for charging (contains Enable input, RFID, UDP,..)")), //
		ENABLE_USER(Doc.of(OpenemsType.BOOLEAN) //
				.text("Enable condition via UDP")), //
		MAX_CURR(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.text("Current preset value via Control pilot")), //
		MAX_CURR_PERCENT(Doc.of(OpenemsType.INTEGER) //
				.text("Current preset value via Control pilot in 0,1% of the PWM value")), //
		CURR_USER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.text("Current preset value of the user via UDP; Default = 63000mA")), //
		CURR_FAILSAFE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.text("Current preset value for the Failsafe function")), //
		TIMEOUT_FAILSAFE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.text("Communication timeout before triggering the Failsafe function")), //
		CURR_TIMER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.text("Shows the current preset value of currtime")), //
		TIMEOUT_CT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.text("Shows the remaining time until the current value is accepted")), //
		OUTPUT(Doc.of(OpenemsType.BOOLEAN) //
				.unit(Unit.ON_OFF) //
				.text("State of the output X2")), //
		INPUT(Doc.of(OpenemsType.BOOLEAN) //
				.unit(Unit.ON_OFF) //
				.text("State of the potential free Enable input X1. When using the input, "
						+ "please pay attention to the information in the installation manual.")), //

		/*
		 * Report 3
		 */
		COS_PHI(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.text("Power factor")), //
		DIP_SWITCH_ERROR_1_3_NOT_SET_FOR_COMM(Doc.of(Level.FAULT) //
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE) //
				.text("Dip-Switch 1.3. for communication must be on")), //
		DIP_SWITCH_ERROR_2_6_NOT_SET_FOR_STATIC_IP(Doc.of(Level.FAULT) //
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE) //
				.text("A static ip is configured. The Dip-Switch 2.6. must be on")), //
		DIP_SWITCH_ERROR_2_6_SET_FOR_DYNAMIC_IP(Doc.of(OpenemsType.BOOLEAN) //
				.text("A dynamic ip is configured. Either the Dip-Switch 2.6. must be off or a static ip has to be configured")), //
		DIP_SWITCH_INFO_2_5_SET_FOR_MASTER_SLAVE_COMM(Doc.of(Level.INFO) //
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE) //
				.text("Master-Slave communication is configured. If this is a normal KEBA that should be not controlled by a KEBA x-series, Dip-Switch 2.5. should be off")), //
		DIP_SWITCH_INFO_2_8_SET_FOR_INSTALLATION(Doc.of(Level.WARNING) //
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE) //
				.text("Installation mode is configured. If the installation has finished, Dip-Switch 2.8. should be off")), //
		PRODUCT_SERIES_IS_NOT_COMPATIBLE(Doc.of(Level.FAULT) //
				.text("Keba e- and b-series cannot be controlled because their software and hardware are not designed for it.")), //
		NO_ENERGY_METER_INSTALLED(Doc.of(Level.INFO) //
				.text("This keba cannot measure energy values, because there is no energy meter in it.")), //
		CHARGINGSTATION_STATE_ERROR(Doc.of(Level.WARNING) //
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
}
