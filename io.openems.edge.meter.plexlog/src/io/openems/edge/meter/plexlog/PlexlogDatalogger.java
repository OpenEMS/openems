package io.openems.edge.meter.plexlog;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;

public interface PlexlogDatalogger extends ElectricityMeter, ModbusComponent, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Sum of production.
		 *
		 * <ul>
		 * <li>Interface: Meter PlexlogDatalogger
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		TOTAL_PRODUCTION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)), //
		
		/**
		 * The exponent of the sum of production.
		 *
		 * <ul>
		 * <li>Interface: Meter PlexlogDatalogger
		 * <li>Type: Integer
		 * <li>Unit: (sum of production) * 10^exponent
		 * </ul>
		 */
		PRODUCTION_EXPONENT(Doc.of(OpenemsType.INTEGER)), //
		
		/**
		 * Sum of consumption.
		 *
		 * <ul>
		 * <li>Interface: Meter PlexlogDatalogger
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		TOTAL_CONSUMPTION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)), //

		/**
		 * The exponent of the sum of consumption.
		 *
		 * <ul>
		 * <li>Interface: Meter PlexlogDatalogger
		 * <li>Type: Integer
		 * <li>Unit: (sum of consumption) * 10^exponent
		 * </ul>
		 */
		CONSUMPTION_EXPONENT(Doc.of(OpenemsType.INTEGER)), //
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
	 * Gets the Consumption Exponent. See {@link ChannelId#CONSUMPTION_EXPONENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getConsumptionExponent() {
		return this.getConsumptionExponentChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CONSUMPTION_EXPONENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getConsumptionExponentChannel() {
		return this.channel(ChannelId.CONSUMPTION_EXPONENT);
	}

	/**
	 * Gets the Production Exponent. See {@link ChannelId#PRODUCTION_EXPONENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getProductionExponent() {
		return this.getProductionExponentChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#PRODUCTION_EXPONENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getProductionExponentChannel() {
		return this.channel(ChannelId.PRODUCTION_EXPONENT);
	}

	/**
	 * Gets the Total Consumption in [Wh]. See {@link ChannelId#TOTAL_CONSUMPTION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTotalConsumption() {
		return this.getTotalConsumptionChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#TOTAL_CONSUMPTION}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTotalConsumptionChannel() {
		return this.channel(ChannelId.TOTAL_CONSUMPTION);
	}

	/**
	 * Gets the Total Production in [Wh]. See {@link ChannelId#TOTAL_PRODUCTION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTotalProduction() {
		return this.getTotalProductionChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#TOTAL_PRODUCTION}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTotalProductionChannel() {
		return this.channel(ChannelId.TOTAL_PRODUCTION);
	}

}
