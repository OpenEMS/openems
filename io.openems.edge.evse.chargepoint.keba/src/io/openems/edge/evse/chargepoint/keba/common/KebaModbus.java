package io.openems.edge.evse.chargepoint.keba.common;

import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.common.types.SemanticVersion;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StringDoc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.keba.modbus.EvcsKebaModbusImpl;
import io.openems.edge.meter.api.ElectricityMeter;

public abstract class KebaModbus extends AbstractOpenemsModbusComponent
		implements Keba, OpenemsComponent, ElectricityMeter {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		ERROR_CODE(Doc.of(INTEGER)), //
		SERIAL_NUMBER(Doc.of(INTEGER)), //
		MAX_CHARGING_CURRENT(Doc.of(INTEGER)//
				.unit(Unit.MILLIAMPERE)), //
		FAILSAFE_CURRENT_SETTING(Doc.of(INTEGER)), //
		FAILSAFE_TIMEOUT_SETTING(Doc.of(INTEGER)), //
		DEVICE_SOFTWARE_OUTDATED(Doc.of(Level.WARNING)//
				.translationKey(EvcsKebaModbusImpl.class, "softwareOutdated")),
		FIRMWARE(new StringDoc().onChannelSetNextValue((keba, value) -> {
			var outdated = false;
			if (value.isDefined()) {
				String firmWare = value.get();
				outdated = !SemanticVersion.fromStringOrZero(firmWare).isAtLeast(SemanticVersion.fromString("1.1.4"));
			}

			keba.channel(KebaModbus.ChannelId.DEVICE_SOFTWARE_OUTDATED).setNextValue(outdated);
		})), //
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

	protected KebaModbus(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}
}
