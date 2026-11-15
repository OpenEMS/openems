package io.openems.edge.protectionrelay.telehaase.na003m64;

import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.Level.INFO;
import static io.openems.common.channel.Unit.MILLIHERTZ;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.SHORT;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.protectionrelay.telehaase.na003m64.enums.ModbusProperties;

public interface Na003ComM64 extends OpenemsComponent {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		DIGITAL_INPUT_1(Doc.of(BOOLEAN)), //
		DIGITAL_INPUT_2(Doc.of(BOOLEAN)), //
		DIGITAL_INPUT_3(Doc.of(BOOLEAN)), //
		DIGITAL_INPUT_4(Doc.of(BOOLEAN)), //
		DIGITAL_INPUT_5(Doc.of(BOOLEAN)), //
		ERROR_LOGIC_CONTACT_REPORTS_CLOSED(Doc.of(INFO)//
				.text("Contactor feedback contact reports closed, although it should be open")), //
		ERROR_LOGIC_CONTACT_REPORTS_OPEN(Doc.of(INFO)//
				.text("Contactor feedback contact reports open, although it should be closed (no error, only info)")), //
		ERROR_LOGIC_ERROR_DELAY_RUNNING(Doc.of(INFO)//
				.text("Error delay running")), //
		ERROR_LOGIC_ERROR_SYSTEM(Doc.of(INFO)//
				.text("Error System")), //
		ERROR_LOGIC_FREQUENCY_CHANGE_RATE(Doc.of(INFO)//
				.text("RoCoF (Rate of Change of Frequency)")), //
		ERROR_LOGIC_GOOD_DELAY_RUNNING(Doc.of(INFO)//
				.text("Good delay running")), //
		ERROR_LOGIC_MASTER_ERROR(Doc.of(INFO)//
				.text("Master error")), //
		ERROR_LOGIC_OVERFREQUENCY(Doc.of(INFO)//
				.text("Overfrequency")), //
		ERROR_LOGIC_OVERVOLTAGE_LL(Doc.of(INFO)//
				.text("Overvoltage Line to Line")), //
		ERROR_LOGIC_OVERVOLTAGE_LN(Doc.of(INFO)//
				.text("Overvoltage Line to Neutral")), //
		ERROR_LOGIC_PHASE_SHIFT(Doc.of(INFO)//
				.text("Phase Shift")), //
		ERROR_LOGIC_REMOTE_SHUTDOWN_SELF_TEST(Doc.of(INFO)//
				.text("Remote shutdown / self-test")), //
		ERROR_LOGIC_TEN_MIN_OVERVOLTAGE(Doc.of(INFO)//
				.text("10-min Overvoltage")), //
		ERROR_LOGIC_UNDERVOLTAGE_LL(Doc.of(INFO)//
				.text("Undervoltage Line to Line")), //
		ERROR_LOGIC_UNDERVOLTAGE_LN(Doc.of(INFO)//
				.text("Undervoltage Line to Neutral")), //
		ERROR_LOGIC_UNDERFREQUENCY(Doc.of(INFO)//
				.text("Underfrequency")), //
		FREQUENCY(Doc.of(INTEGER)//
				.unit(MILLIHERTZ)), //
		GOOD_COUNTDOWN(Doc.of(INTEGER)), //
		LED_REL_1(Doc.of(BOOLEAN)), //
		LED_REL_2(Doc.of(BOOLEAN)), //
		LED_REL_3(Doc.of(BOOLEAN)), //
		MODBUS_ADDRESS(Doc.of(SHORT)//
				.accessMode(READ_WRITE)), //
		MODBUS_BAUDRATE(Doc.of(BOOLEAN)//
				.accessMode(READ_WRITE)), //
		MODBUS_PROPERTIES(Doc.of(ModbusProperties.values())//
				.accessMode(READ_WRITE)), //
		U_AVG_1(Doc.of(INTEGER)), //
		U_AVG_2(Doc.of(INTEGER)), //
		U_AVG_3(Doc.of(INTEGER)), //
		U_DELTA_1_2(Doc.of(INTEGER)), //
		U_DELTA_2_3(Doc.of(INTEGER)), //
		U_DELTA_3_1(Doc.of(INTEGER)), //
		U_STAR_1(Doc.of(INTEGER)), //
		U_STAR_2(Doc.of(INTEGER)), //
		U_STAR_3(Doc.of(INTEGER)), //
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