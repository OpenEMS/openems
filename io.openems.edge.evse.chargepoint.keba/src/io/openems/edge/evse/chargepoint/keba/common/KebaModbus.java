package io.openems.edge.evse.chargepoint.keba.common;

import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.common.types.SemanticVersion;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.keba.modbus.EvcsKebaModbusImpl;
import io.openems.edge.meter.api.ElectricityMeter;

public abstract class KebaModbus extends AbstractOpenemsModbusComponent
		implements Keba, OpenemsComponent, ElectricityMeter {

	/** Show DEVICE_SOFTWARE_OUTDATED for versions strictly smaller than 1.1.9. */
	public static final SemanticVersion FIRMWARE_OUTDATED_WARNING = new SemanticVersion(1, 1, 9);
	/** Fix known bug with energy scale factors in versions below 1.2.1. */
	public static final SemanticVersion FIRMWARE_ENERGY_SCALE_MIN_BUG = new SemanticVersion(1, 2, 1);

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		ERROR_CODE(Doc.of(INTEGER)), //
		SERIAL_NUMBER(Doc.of(INTEGER)), //
		MAX_CHARGING_CURRENT(Doc.of(INTEGER)//
				.unit(Unit.MILLIAMPERE)), //
		FAILSAFE_CURRENT_SETTING(Doc.of(INTEGER)), //
		FAILSAFE_TIMEOUT_SETTING(Doc.of(INTEGER)), //
		DEVICE_SOFTWARE_OUTDATED(Doc.of(Level.WARNING)//
				.translationKey(EvcsKebaModbusImpl.class, "softwareOutdated")),
		FIRMWARE(Doc.of(OpenemsType.STRING)), //
		FIRMWARE_MAJOR(Doc.of(OpenemsType.INTEGER)), //
		FIRMWARE_MINOR(Doc.of(OpenemsType.INTEGER)), //
		FIRMWARE_PATCH(Doc.of(OpenemsType.INTEGER)), //

		PTAF_PRODUCT_FAMILY(Doc.of(ProductTypeAndFeatures.ProductFamily.values())), //
		PTAF_DEVICE_CURRENT(Doc.of(ProductTypeAndFeatures.DeviceCurrent.values())), //
		PTAF_CONNECTOR(Doc.of(ProductTypeAndFeatures.Connector.values())), //
		PTAF_PHASES(Doc.of(ProductTypeAndFeatures.Phases.values())), //
		PTAF_METERING(Doc.of(ProductTypeAndFeatures.Metering.values())), //
		PTAF_RFID(Doc.of(ProductTypeAndFeatures.Rfid.values())), //
		PTAF_BUTTON(Doc.of(ProductTypeAndFeatures.Button.values())), //
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
	 * Gets the Channel for {@link ChannelId#MAX_CHARGING_CURRENT}.
	 *
	 * @return the Channel
	 */
	protected IntegerReadChannel getMaxChargingCurrentChannel() {
		return this.channel(ChannelId.MAX_CHARGING_CURRENT);
	}

	/**
	 * Gets the maximum current allowed by the hardware in [mA]. See
	 * {@link ChannelId#MAX_CHARGING_CURRENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	protected Value<Integer> getMaxChargingCurrent() {
		return this.getMaxChargingCurrentChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#FIRMWARE}.
	 *
	 * @return the Channel
	 */
	protected StringReadChannel getFirmwareChannel() {
		return this.channel(ChannelId.FIRMWARE);
	}

	/**
	 * Gets the firmware. See {@link ChannelId#FIRMWARE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public Value<String> getFirmware() {
		return this.getFirmwareChannel().value();
	}

	protected KebaModbus(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}
}
