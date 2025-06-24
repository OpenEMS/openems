package io.openems.edge.evcs.keba.p40;

import static io.openems.common.channel.AccessMode.WRITE_ONLY;
import static io.openems.common.channel.Unit.MILLIAMPERE;
import static io.openems.common.types.OpenemsType.INTEGER;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.keba.common.R2Plug;
import io.openems.edge.meter.api.ElectricityMeter;

public interface EvcsKebaP40 extends ManagedEvcs, Evcs, ElectricityMeter, OpenemsComponent, EventHandler, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		PLUG(Doc.of(R2Plug.values())), //
		ERROR_CODE(Doc.of(OpenemsType.INTEGER)), //
		SERIAL_NUMBER(Doc.of(OpenemsType.INTEGER)), //
		PRODUCT_TYPE(Doc.of(OpenemsType.INTEGER)), //
		FIRMWARE(Doc.of(OpenemsType.STRING)), //
		POWER_FACTOR(Doc.of(OpenemsType.FLOAT)), //
		MAX_CHARGING_CURRENT(Doc.of(OpenemsType.INTEGER)), //
		MAX_HARDWARE_CURRENT(Doc.of(OpenemsType.INTEGER)), //
		RFID(Doc.of(OpenemsType.STRING)), //
		PHASE_SWITCH_SOURCE(Doc.of(OpenemsType.INTEGER)), //
		PHASE_SWITCH_STATE(Doc.of(OpenemsType.INTEGER)), //
		FAILSAFE_CURRENT_SETTING(Doc.of(OpenemsType.INTEGER)), //
		FAILSAFE_TIMEOUT_SETTING(Doc.of(OpenemsType.INTEGER)), //
		DEBUG_SET_CHARGING_CURRENT(Doc.of(INTEGER) //
				.unit(MILLIAMPERE)), //
		SET_CHARGING_CURRENT(Doc.of(INTEGER) //
				.unit(MILLIAMPERE) //
				.accessMode(WRITE_ONLY) //
				.onChannelSetNextWriteMirrorToDebugChannel(DEBUG_SET_CHARGING_CURRENT)),
		DEBUG_SET_ENABLE(Doc.of(INTEGER)), //
		// 0: Disable charging station (Suspended mode)
		// 1: Enable charging station (Charging)
		SET_ENABLE(Doc.of(INTEGER) //
				.accessMode(WRITE_ONLY) //
				.onChannelSetNextWriteMirrorToDebugChannel(DEBUG_SET_ENABLE)),

		PTAF_PRODUCT_TYPE(Doc.of(ProductTypeAndFeatures.ProductType.values())), //
		PTAF_CABLE_OR_SOCKET(Doc.of(ProductTypeAndFeatures.CableOrSocket.values())), //
		PTAF_SUPPORTED_CURRENT(Doc.of(ProductTypeAndFeatures.SupportedCurrent.values())), //
		PTAF_DEVICE_SERIES(Doc.of(ProductTypeAndFeatures.DeviceSeries.values())), //
		PTAF_ENERGY_METER(Doc.of(ProductTypeAndFeatures.EnergyMeter.values())), //
		PTAF_AUTHORIZATION(Doc.of(ProductTypeAndFeatures.Authorization.values())), //
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

	@Override
	public default ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Evcs.getModbusSlaveNatureTable(accessMode), //
				ManagedEvcs.getModbusSlaveNatureTable(accessMode), //
				this.getModbusSlaveNatureTable(accessMode));
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_CHARGING_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetEnableChannel() {
		return this.channel(ChannelId.SET_ENABLE);
	}

	/**
	 * Sets the next Write Value for {@link ChannelId#SET_ENABLE}.
	 * 
	 * @param setEnable one for is enabled
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetEnable(int setEnable) throws OpenemsNamedException {
		this.getSetEnableChannel().setNextWriteValue(setEnable);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_CHARGING_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetChargingCurrentChannel() {
		return this.channel(ChannelId.SET_CHARGING_CURRENT);
	}

	/**
	 * Sets the next Write Value for {@link ChannelId#SET_CHARGING_CURRENT}.
	 * 
	 * @param current current to be set
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetChargingCurrent(int current) throws OpenemsNamedException {
		this.getSetChargingCurrentChannel().setNextWriteValue(current);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_CHARGING_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxChargingCurrentChannel() {
		return this.channel(ChannelId.MAX_CHARGING_CURRENT);
	}

	/**
	 * Gets the maximum current allowed by the hardware in [A]. See
	 * {@link ChannelId#MAX_CHARGING_CURRENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxChargingCurrent() {
		return this.getMaxChargingCurrentChannel().value();
	}

	/**
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	private ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(EvcsKebaP40.class, accessMode, 300) //
				.build();
	}
}
