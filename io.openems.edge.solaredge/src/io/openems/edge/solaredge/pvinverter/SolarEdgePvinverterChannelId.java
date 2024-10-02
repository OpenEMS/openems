package io.openems.edge.solaredge.pvinverter;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
//import io.openems.edge.ess.api.HybridEss.ChannelId;

public interface SolarEdgePvinverterChannelId extends OpenemsComponent {
	
	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {	
		/**
		 * State of Charge.
		 *
		 * <ul>
		 * <li>Interface: SolarEdgeHybridPvinverter
		 * <li>Type: Integer
		 * <li>Unit: %
		 * <li>Range: 0..100
		 * </ul>
		 */
		GRID_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),
		
		/**
		 * State of Charge.
		 *
		 * <ul>
		 * <li>Interface: SolarEdgeHybridPvinverter
		 * <li>Type: Integer
		 * <li>Unit: Exponent for Grid-Power
		 * <li>Range: 0..100
		 * </ul>
		 */
		GRID_POWER_SCALE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.persistencePriority(PersistencePriority.HIGH)),		
		
		/**
		 * State of Charge.
		 *
		 * <ul>
		 * <li>Interface: SolarEdgeHybridPvinverter
		 * <li>Type: Integer
		 * <li>Unit: %
		 * <li>Range: 0..100
		 * </ul>
		 */
		CONSUMPTION_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),
		
		/**
		 * State of Charge.
		 *
		 * <ul>
		 * <li>Interface: SolarEdgeHybridPvinverter
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		PRODUCTION_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),		
		
		/**
		 * DC Discharge Power.
		 *
		 * <ul>
		 * <li>Interface: SolarEdgeHybridPvinverter
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * <li>This is the
		 * {@link io.openems.edge.ess.api.SymmetricEss.ChannelId#ACTIVE_POWER} minus
		 * {@link io.openems.edge.ess.dccharger.api.EssDcCharger.ChannelId#ACTUAL_POWER},
		 * i.e. the power that is actually charged to or discharged from the battery.
		 * </ul>
		 */
		DC_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Actual AC-side battery discharge power of Energy Storage System. " //
						+ "Negative values for charge; positive for discharge"));
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
	 * Gets the Channel for {@link ChannelId#DC_DISCHARGE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getConsumptionPowerChannel() {
		return this.channel(ChannelId.CONSUMPTION_POWER);
	}

	/**
	 * Gets the DC Discharge Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#DC_DISCHARGE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getConsumptionPower() {
		return this.getConsumptionPowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DC_DISCHARGE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setConsumptionPower(Integer value) {
		this.getConsumptionPowerChannel().setNextValue(value);
	}	
	
	/**
	 * Gets the Channel for {@link ChannelId#PRODUCTION_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getProductionPowerChannel() {
		return this.channel(ChannelId.PRODUCTION_POWER);
	}

	/**
	 * Gets the DC Discharge Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#PRODUCTION_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getProductionPower() {
		return this.getProductionPowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PRODUCTION_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionPower(Integer value) {
		this.getProductionPowerChannel().setNextValue(value);
	}
	
	
	

	/**
	 * Gets the Channel for {@link ChannelId#DC_DISCHARGE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcDischargePowerChannel() {
		return this.channel(ChannelId.DC_DISCHARGE_POWER);
	}

	/**
	 * Gets the DC Discharge Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#DC_DISCHARGE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcDischargePower() {
		return this.getDcDischargePowerChannel().value();
	}

	
	
	/**
	 * Gets the Channel for {@link ChannelId#DC_DISCHARGE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGridPowerScaleChannel() {
		return this.channel(ChannelId.GRID_POWER_SCALE);
	}

	/**
	 * Gets the DC Discharge Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#DC_DISCHARGE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGridPowerScale() {
		return this.getGridPowerScaleChannel().value();
	}
	
	
	/**
	 * Gets the Channel for {@link ChannelId#DC_DISCHARGE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGridPowerChannel() {
		return this.channel(ChannelId.GRID_POWER);
	}

	/**
	 * Gets the DC Discharge Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#DC_DISCHARGE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGridPower() {
		return this.getGridPowerChannel().value();
	}

}
