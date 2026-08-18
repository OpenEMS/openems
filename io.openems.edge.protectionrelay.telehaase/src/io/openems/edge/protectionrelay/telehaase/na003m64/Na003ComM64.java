package io.openems.edge.protectionrelay.telehaase.na003m64;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.protectionrelay.telehaase.na003m64.enums.ContactState;
import io.openems.edge.protectionrelay.telehaase.na003m64.enums.ModbusProperties;

public interface Na003ComM64 extends OpenemsComponent {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CEN_LN_U_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Important for C10/11 belgium (activation narrow frequency window)")), //
		CEN_LN_U_OFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Important for C10/11 belgium (activation narrow frequency window)")), //
		CEN_LN_U_ON(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Important for C10/11 belgium (activation narrow frequency window)")), //
		CEN_LN_U_SEL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Important for C10/11 belgium (activation narrow frequency window)")), //
		CEN_URES_O_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Important for C10/11 belgium (activation narrow frequency window)")), //
		CEN_URES_O_OFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Important for C10/11 belgium (activation narrow frequency window)")), //
		CEN_URES_O_ON(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Important for C10/11 belgium (activation narrow frequency window)")), //
		CEN_URES_O_SEL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Important for C10/11 belgium (activation narrow frequency window)")), //
		CONTACT(Doc.of(ContactState.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		CONTACT_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		DIGITAL_INPUT_1(Doc.of(OpenemsType.BOOLEAN)), //
		DIGITAL_INPUT_2(Doc.of(OpenemsType.BOOLEAN)), //
		DIGITAL_INPUT_3(Doc.of(OpenemsType.BOOLEAN)), //
		DIGITAL_INPUT_3_STATE(Doc.of(ContactState.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		DIGITAL_INPUT_4(Doc.of(OpenemsType.BOOLEAN)), //
		DIGITAL_INPUT_5(Doc.of(OpenemsType.BOOLEAN)), //
		ERROR_LOGIC_CONTACT_REPORTS_CLOSED(Doc.of(Level.INFO) //
				.text("Contactor feedback contact reports closed, although it should be open")), //
		ERROR_LOGIC_CONTACT_REPORTS_OPEN(Doc.of(Level.INFO) //
				.text("Contactor feedback contact reports open, although it should be closed (no error, only info)")), //
		ERROR_LOGIC_ERROR_DELAY_RUNNING(Doc.of(Level.INFO) //
				.text("Error delay running")), //
		ERROR_LOGIC_ERROR_SYSTEM(Doc.of(Level.INFO) //
				.text("Error System")), //
		ERROR_LOGIC_FREQUENCY_CHANGE_RATE(Doc.of(Level.INFO) //
				.text("RoCoF (Rate of Change of Frequency)")), //
		ERROR_LOGIC_GOOD_DELAY_RUNNING(Doc.of(Level.INFO) //
				.text("Good delay running")), //
		ERROR_LOGIC_MASTER_ERROR(Doc.of(Level.INFO) //
				.text("Master error")), //
		ERROR_LOGIC_OVERFREQUENCY(Doc.of(Level.INFO) //
				.text("Overfrequency")), //
		ERROR_LOGIC_OVERVOLTAGE_LL(Doc.of(Level.INFO) //
				.text("Overvoltage Line to Line")), //
		ERROR_LOGIC_OVERVOLTAGE_LN(Doc.of(Level.INFO) //
				.text("Overvoltage Line to Neutral")), //
		ERROR_LOGIC_PHASE_SHIFT(Doc.of(Level.INFO) //
				.text("Phase Shift")), //
		ERROR_LOGIC_REMOTE_SHUTDOWN_SELF_TEST(Doc.of(Level.INFO) //
				.text("Remote shutdown / self-test")), //
		ERROR_LOGIC_TEN_MIN_OVERVOLTAGE(Doc.of(Level.INFO) //
				.text("10-min Overvoltage")), //
		ERROR_LOGIC_UNDERVOLTAGE_LL(Doc.of(Level.INFO) //
				.text("Undervoltage Line to Line")), //
		ERROR_LOGIC_UNDERVOLTAGE_LN(Doc.of(Level.INFO) //
				.text("Undervoltage Line to Neutral")), //
		ERROR_LOGIC_UNDERFREQUENCY(Doc.of(Level.INFO) //
				.text("Underfrequency")), //
		FREQUENCY(Doc.of(OpenemsType.INTEGER)), //
		FREQUENCY_CHANGE_RATE_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		FREQUENCY_CHANGE_RATE_OFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		FREQUENCY_CHANGE_RATE_ON(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		FREQUENCY_CHANGE_RATE_SEL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		FREQUENCY_RANDOM_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		FREQUENCY_RANDOM_OFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		FREQUENCY_RANDOM_ON(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		FREQUENCY_RANDOM_SEL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		GOOD_COUNTDOWN(Doc.of(OpenemsType.INTEGER)), //
		LED_REL_1(Doc.of(OpenemsType.BOOLEAN)), //
		LED_REL_2(Doc.of(OpenemsType.BOOLEAN)), //
		LED_REL_3(Doc.of(OpenemsType.BOOLEAN)), //
		MODBUS_ADDRESS(Doc.of(OpenemsType.SHORT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Modbus Slave individual address (1-247), default 1")), //
		MODBUS_BAUDRATE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("True: 19200 (default), False: 9600")), //
		MODBUS_ON_OFF(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("True: Modbus ON, False: Modbus OFF")), //
		MODBUS_PROPERTIES(Doc.of(ModbusProperties.values()) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("[0]: 8 data bits, even parity, 1 stop bit (default), [1]: 8 data bits, odd parity, 1 stop bit, [2]: 8 data bits, no parity, 2 stop bit, [3]: 8 data bits, no parity, 1 stop bit (NOT conform)")), //
		ON_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		ON_DELAY_R(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		ON_DELAY_R_SEL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERFREQUENCY_1_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERFREQUENCY_1_OFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERFREQUENCY_1_ON(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERFREQUENCY_1_SEL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERFREQUENCY_2_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERFREQUENCY_2_OFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERFREQUENCY_2_ON(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERFREQUENCY_2_SEL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERFREQUENCY_3_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERFREQUENCY_3_OFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERFREQUENCY_3_ON(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERFREQUENCY_3_SEL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERVOLTAGE_1_LINE_TO_LINE_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERVOLTAGE_1_LINE_TO_LINE_OFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERVOLTAGE_1_LINE_TO_LINE_ON(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERVOLTAGE_1_LINE_TO_LINE_SEL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERVOLTAGE_1_LINE_TO_NEUTRAL_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERVOLTAGE_1_LINE_TO_NEUTRAL_OFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERVOLTAGE_1_LINE_TO_NEUTRAL_ON(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERVOLTAGE_1_LINE_TO_NEUTRAL_SEL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERVOLTAGE_10_MIN_AVG_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERVOLTAGE_10_MIN_AVG_OFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERVOLTAGE_10_MIN_AVG_ON(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERVOLTAGE_10_MIN_AVG_SEL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERVOLTAGE_2_LINE_TO_NEUTRAL_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERVOLTAGE_2_LINE_TO_NEUTRAL_OFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERVOLTAGE_2_LINE_TO_NEUTRAL_ON(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERVOLTAGE_2_LINE_TO_NEUTRAL_SEL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERVOLTAGE_2_LINE_TO_LINE_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERVOLTAGE_2_LINE_TO_LINE_OFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERVOLTAGE_2_LINE_TO_LINE_ON(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OVERVOLTAGE_2_LINE_TO_LINE_SEL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		PHASE_SHIFT_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		PHASE_SHIFT_OFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		PHASE_SHIFT_ON(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		PHASE_SHIFT_SEL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		U_AVG_1(Doc.of(OpenemsType.INTEGER)), //
		U_AVG_2(Doc.of(OpenemsType.INTEGER)), //
		U_AVG_3(Doc.of(OpenemsType.INTEGER)), //
		U_DELTA_1_2(Doc.of(OpenemsType.INTEGER)), //
		U_DELTA_2_3(Doc.of(OpenemsType.INTEGER)), //
		U_DELTA_3_1(Doc.of(OpenemsType.INTEGER)), //
		U_STAR_1(Doc.of(OpenemsType.INTEGER)), //
		U_STAR_2(Doc.of(OpenemsType.INTEGER)), //
		U_STAR_3(Doc.of(OpenemsType.INTEGER)), //
		U_ZERO_O_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Important for C10/11 belgium (activation narrow frequency window)")), //
		U_ZERO_O_OFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Important for C10/11 belgium (activation narrow frequency window)")), //
		U_ZERO_O_ON(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Important for C10/11 belgium (activation narrow frequency window)")), //
		U_ZERO_O_SEL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Important for C10/11 belgium (activation narrow frequency window)")), //
		UNDERFREQUENCY_1_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERFREQUENCY_1_OFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERFREQUENCY_1_ON(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERFREQUENCY_1_SEL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERFREQUENCY_2_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERFREQUENCY_2_OFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERFREQUENCY_2_ON(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERFREQUENCY_2_SEL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERFREQUENCY_3_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERFREQUENCY_3_OFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERFREQUENCY_3_ON(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERFREQUENCY_3_SEL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERVOLTAGE_1_LINE_TO_LINE_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERVOLTAGE_1_LINE_TO_LINE_OFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERVOLTAGE_1_LINE_TO_LINE_ON(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERVOLTAGE_1_LINE_TO_LINE_SEL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERVOLTAGE_1_LINE_TO_NEUTRAL_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERVOLTAGE_1_LINE_TO_NEUTRAL_OFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERVOLTAGE_1_LINE_TO_NEUTRAL_ON(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERVOLTAGE_1_LINE_TO_NEUTRAL_SEL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERVOLTAGE_2_LINE_TO_LINE_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERVOLTAGE_2_LINE_TO_LINE_OFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERVOLTAGE_2_LINE_TO_LINE_ON(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERVOLTAGE_2_LINE_TO_LINE_SEL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERVOLTAGE_2_LINE_TO_NEUTRAL_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERVOLTAGE_2_LINE_TO_NEUTRAL_OFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERVOLTAGE_2_LINE_TO_NEUTRAL_ON(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDERVOLTAGE_2_LINE_TO_NEUTRAL_SEL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		;

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

}
