package io.openems.edge.gassensor.vigilex;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface VigilexVigiDt1 extends OpenemsComponent, ModbusComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		RE1_STATE(Doc.of(Level.WARNING)//
				.text("Alarm relay RE1 active (first threshold exceeded)")), //
		RE2_STATE(Doc.of(Level.FAULT)//
				.text("Alarm relay RE2 active (second threshold exceeded)")), //
		RE1_DELAY(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)//
				.accessMode(AccessMode.READ_WRITE)), //
		RE2_DELAY(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)//
				.accessMode(AccessMode.READ_WRITE)), //
		HYDROGEN_RE1_LOW_SETPOINT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)//
				.accessMode(AccessMode.READ_WRITE)), //
		HYDROGEN_RE1_HIGH_SETPOINT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)//
				.accessMode(AccessMode.READ_WRITE)), //
		HYDROGEN_RE2_LOW_SETPOINT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)//
				.accessMode(AccessMode.READ_WRITE)), //
		HYDROGEN_RE2_HIGH_SETPOINT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)//
				.accessMode(AccessMode.READ_WRITE)), //
		VOC_RE1_LOW_SETPOINT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)//
				.accessMode(AccessMode.READ_WRITE)), //
		VOC_RE1_HIGH_SETPOINT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)//
				.accessMode(AccessMode.READ_WRITE)), //
		VOC_RE2_LOW_SETPOINT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)//
				.accessMode(AccessMode.READ_WRITE)), //
		VOC_RE2_HIGH_SETPOINT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)//
				.accessMode(AccessMode.READ_WRITE)), //
		TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		HUMIDITY(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PERCENT)), //
		// Volatile Organic Compounds
		VOC(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)), //
		HYDROGEN(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)//
				.text("% LEL")), //
		;

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