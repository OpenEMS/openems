package io.openems.edge.deye.common;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.timedata.api.TimedataProvider;
import org.osgi.service.event.EventHandler;

public interface DeyeSunHybrid
		extends ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler, ModbusSlave, TimedataProvider {

	/**
	 * Gets the Modbus Unit-ID.
	 *
	 * @return the Unit-ID
	 */
	public Integer getUnitId();

	/**
	 * Gets the Modbus-Bridge Component-ID, i.e. "modbus0".
	 *
	 * @return the Component-ID
	 */
	public String getModbusBridgeId();

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// EnumReadChannels
		SERIAL_NUMBER(Doc.of(OpenemsType.STRING) //
				.persistencePriority(PersistencePriority.HIGH) //
				.accessMode(AccessMode.READ_ONLY)),
		SURPLUS_FEED_IN_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //

		// Gen Port Use Channels
		// AC 1/28/2024
		SET_GRID_LOAD_OFF_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT) //
				.accessMode(AccessMode.WRITE_ONLY)), // ), //

	

		// EnumWriteChannels
		SET_WORK_STATE(Doc.of(SetWorkState.values()) //
				.accessMode(AccessMode.WRITE_ONLY)), //

		// IntegerWriteChannel
		SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.accessMode(AccessMode.WRITE_ONLY)), //

		SET_GEN_PEAK_SHAVING_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_GRID_PEAK_SHAVING_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		CT_RATIO(Doc.of(OpenemsType.INTEGER)), //
		INVERTER_RUN_STATE(Doc.of(OpenemsType.INTEGER)), //

		// LongReadChannel
		ORIGINAL_ACTIVE_CHARGE_ENERGY(Doc.of(OpenemsType.LONG)), //
		ORIGINAL_ACTIVE_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG)), //

		// IntegerReadChannels
		ORIGINAL_ALLOWED_CHARGE_POWER(new IntegerDoc() //
				.onChannelUpdate((self, newValue) -> {
					// on each Update to the channel -> set the ALLOWED_CHARGE_POWER value with a
					// delta of max 500
					IntegerReadChannel currentValueChannel = self
							.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER);
					var originalValue = newValue.asOptional();
					var currentValue = currentValueChannel.value().asOptional();
					final int value;
					if (!originalValue.isPresent() && !currentValue.isPresent()) {
						value = 0;
					} else if (originalValue.isPresent() && !currentValue.isPresent()) {
						value = originalValue.get();
					} else if (!originalValue.isPresent() && currentValue.isPresent()) {
						value = currentValue.get();
					} else {
						value = Math.max(originalValue.get(), currentValue.get() - 500);
					}
					currentValueChannel.setNextValue(value);
				})), //

		ORIGINAL_ALLOWED_DISCHARGE_POWER(new IntegerDoc() //
				.onChannelUpdate((self, newValue) -> {
					// on each Update to the channel -> set the ALLOWED_DISCHARGE_POWER value with a
					// delta of max 500
					IntegerReadChannel currentValueChannel = self
							.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER);
					var originalValue = newValue.asOptional();
					var currentValue = currentValueChannel.value().asOptional();
					final int value;
					if (!originalValue.isPresent() && !currentValue.isPresent()) {
						value = 0;
					} else if (originalValue.isPresent() && !currentValue.isPresent()) {
						value = originalValue.get();
					} else if (!originalValue.isPresent() && currentValue.isPresent()) {
						value = currentValue.get();
					} else {
						value = Math.min(originalValue.get(), currentValue.get() + 500);
					}
					currentValueChannel.setNextValue(value);
				})), //

	
		APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)), //


		// StateChannels
		SYSTEM_ERROR(Doc.of(Level.FAULT) //
				.onInit(new StateChannel.TriggerOnAny(SystemErrorChannelId.values()))
				.text("System-Error. More information at: https://deyeinverter.com/")), //
		INSUFFICIENT_GRID_PARAMTERS(Doc.of(Level.FAULT) //
				.onInit(new StateChannel.TriggerOnAny(InsufficientGridParametersChannelId.values()))
				.text("Insufficient Grid Parameters. More information at: https://deyeinverter.com/")), //
		POWER_DECREASE_CAUSED_BY_OVERTEMPERATURE(Doc.of(Level.FAULT) //
				.onInit(new StateChannel.TriggerOnAny(PowerDecreaseCausedByOvertemperatureChannelId.values()))
				.text("Power Decrease caused by Overtemperature. More information at:  https://deyeinverter.com/")), //
		EMERGENCY_STOP_ACTIVATED(Doc.of(Level.WARNING) //
				.text("Emergency Stop has been activated. More information at:  https://deyeinverter.com/")), //
		KEY_MANUAL_ACTIVATED(Doc.of(Level.WARNING) //
				.text("Key Manual has been activated. More information at:  https://deyeinverter.com/")), //
		BECU_UNIT_DEFECTIVE(Doc.of(Level.FAULT) //
				.text("BECU Unit is defective. More information at:  https://deyeinverter.com/")), //
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

	/**
	 * Source-Channels for {@link ChannelId#SYSTEM_ERROR}.
	 */
	public static enum SystemErrorChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_149(Doc.of(OpenemsType.BOOLEAN) //
				.text("HighVoltageSideVoltageChangeUnconventionally")) //
		;

		private final Doc doc;

		private SystemErrorChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;

		}
	}

	/**
	 * Source-Channels for {@link ChannelId#INSUFFICIENT_GRID_PARAMTERS}.
	 */
	public static enum InsufficientGridParametersChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_84(Doc.of(OpenemsType.BOOLEAN) //
				.text("Phase3InverterVoltageGeneralOvervoltageProtection")), //
		;

		private final Doc doc;

		private InsufficientGridParametersChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;

		}
	}

	/**
	 * Source-Channels for
	 * {@link ChannelId#POWER_DECREASE_CAUSED_BY_OVERTEMPERATURE}.
	 */
	public static enum PowerDecreaseCausedByOvertemperatureChannelId
			implements io.openems.edge.common.channel.ChannelId {
		STATE_146(Doc.of(OpenemsType.BOOLEAN) //
				.text("Fan4StartupFailed"));

		private final Doc doc;

		private PowerDecreaseCausedByOvertemperatureChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;

		}
	}

}
