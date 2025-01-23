package io.openems.edge.common.meta;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.PersistencePriority.VERY_LOW;
import static io.openems.common.channel.Unit.SECONDS;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.LONG;
import static io.openems.common.types.OpenemsType.STRING;

import io.openems.common.OpenemsConstants;
import io.openems.common.channel.AccessMode;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.currency.Currency;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;

public interface Meta extends ModbusSlave {

	public static final String SINGLETON_SERVICE_PID = "Core.Meta";
	public static final String SINGLETON_COMPONENT_ID = "_meta";

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * OpenEMS Version.
		 *
		 * <ul>
		 * <li>Interface: Meta
		 * <li>Type: String
		 * </ul>
		 */
		VERSION(Doc.of(STRING) //
				.persistencePriority(HIGH)),
		/**
		 * System Time: seconds since 1st January 1970 00:00:00 UTC.
		 *
		 * <ul>
		 * <li>Interface: Meta
		 * <li>Type: Long
		 * </ul>
		 */
		SYSTEM_TIME_UTC(Doc.of(LONG) //
				.unit(SECONDS) //
				.text("System Time: seconds since 1st January 1970 00:00:00 UTC") //
				.persistencePriority(VERY_LOW)),
		/**
		 * Edge currency.
		 * 
		 * <ul>
		 * <li>Interface: Meta
		 * <li>Type: Currency
		 * </ul>
		 */
		CURRENCY(Doc.of(Currency.values()) //
				.persistencePriority(HIGH)),

		/**
		 * Is it allowed to charge the ESS from Grid?.
		 * 
		 * <ul>
		 * <li>Interface: Meta
		 * <li>Type: Boolean
		 * </ul>
		 */
		IS_ESS_CHARGE_FROM_GRID_ALLOWED(Doc.of(BOOLEAN) //
				.persistencePriority(HIGH));

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
	 * Provides a default implementation for
	 * {@link ModbusSlave#getModbusSlaveTable(AccessMode)}.
	 * 
	 * @param accessMode the {@link AccessMode}
	 * @param oem        the {@link OpenemsEdgeOem}
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public static ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode, OpenemsEdgeOem oem) {
		return new ModbusSlaveTable(//
				ModbusSlaveNatureTable.of(Meta.class, accessMode, 199) //
						.uint16(0, "OpenEMS Version Major", OpenemsConstants.VERSION_MAJOR) //
						.uint16(1, "OpenEMS Version Minor", OpenemsConstants.VERSION_MINOR) //
						.uint16(2, "OpenEMS Version Patch", OpenemsConstants.VERSION_PATCH) //
						.string16(3, "Manufacturer", oem.getManufacturer()) //
						.string16(19, "Manufacturer Model", oem.getManufacturerModel()) //
						.string16(35, "Manufacturer Options", oem.getManufacturerOptions()) //
						.string16(51, "Manufacturer Version", oem.getManufacturerVersion()) //
						.string16(67, "Manufacturer Serial Number", oem.getManufacturerSerialNumber()) //
						.string16(83, "Manufacturer EMS Serial Number", oem.getManufacturerEmsSerialNumber()) //
						.channel(99, ChannelId.SYSTEM_TIME_UTC, ModbusType.UINT64) //
						.build());
	}

	/**
	 * Gets the Channel for {@link ChannelId#CURRENCY}.
	 *
	 * @return the Channel
	 */
	public default EnumReadChannel getCurrencyChannel() {
		return this.channel(ChannelId.CURRENCY);
	}

	/**
	 * Gets the Currency. See {@link ChannelId#CURRENCY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Currency getCurrency() {
		return this.getCurrencyChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CURRENCY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCurrency(Currency value) {
		this.getCurrencyChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#IS_ESS_CHARGE_FROM_GRID_ALLOWED}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getIsEssChargeFromGridAllowedChannel() {
		return this.channel(ChannelId.IS_ESS_CHARGE_FROM_GRID_ALLOWED);
	}

	/**
	 * Gets whether charging the ESS from grid is allowed. See
	 * {@link ChannelId#IS_ESS_CHARGE_FROM_GRID_ALLOWED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default boolean getIsEssChargeFromGridAllowed() {
		return this.getIsEssChargeFromGridAllowedChannel().value().orElse(false);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#IS_ESS_CHARGE_FROM_GRID_ALLOWED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setIsEssChargeFromGridAllowed(boolean value) {
		this.getIsEssChargeFromGridAllowedChannel().setNextValue(value);
	}
}
