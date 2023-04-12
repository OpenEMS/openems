package io.openems.edge.solaredge.charger;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.dccharger.api.EssDcCharger;


public interface SolaredgeDcCharger extends EssDcCharger, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		DEBUG_SET_PV_POWER_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),

		/**
		 * Curtail PV. Careful: this channel is shared between both Chargers.
		 */
		SET_PV_POWER_LIMIT(new IntegerDoc() //
				
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onInit(new IntegerWriteChannel.MirrorToDebugChannel(ChannelId.DEBUG_SET_PV_POWER_LIMIT))), //
		
		/**
		 * Grid-Mode.
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Integer/Enum
		 * <li>Range: 0=Undefined, 1=On-Grid, 2=Off-Grid
		 * </ul>
		 */
		GRID_MODE(Doc.of(GridMode.values()) //
				.persistencePriority(PersistencePriority.HIGH) //
		),	
		
		/**
		 * Power from Grid. Used to calculate pv production
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		POWER_DC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),
		
		
		/**
		 * Power from Grid. Used to calculate pv production
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		POWER_DC_SCALE(Doc.of(OpenemsType.INTEGER) //
				
				.persistencePriority(PersistencePriority.HIGH)),	
		 
	
		/**
		 * Production Power.
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: ?
		 * <li>This is the AC-Production Power coming out of the Inverter
		 * 
		 * It should be ACTUAL_POWER (PV Production Power) 	minus
		 * DC_DISCHARGE_POWER/"instantaneous_power" (+/-)
		 * </ul>
		 */
		PRODUCTION_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),	
		
		
		//GRID_MODE(Doc.of(OpenemsType.STRING) //
		//		.unit(Unit.WATT_HOURS)), 

		// LongReadChannel
		BMS_DCDC_OUTPUT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)),
		
		/**
		 * DC-Discharge Power.
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * <li>This is the instantaneous power to or from the battery
		 * </ul>
		 */
		DC_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH))	
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
	
	//######################
	/**
	 * Gets the Channel for {@link ChannelId#POWER_DC}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcPowerChannel() {
		return this.channel(ChannelId.POWER_DC);
	}

	/**
	 * DC Power Channel {@link ChannelId#POWER_DC_SCALE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcPower() {
		return this.getDcPowerChannel().value();
	}	
	
	//######################
	/**
	 * Gets the Channel for {@link ChannelId#CONTROL_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcPowerScaleChannel() {
		return this.channel(ChannelId.POWER_DC_SCALE);
	}

	/**
	 * Is the Energy Storage System On-Grid? See {@link ChannelId#CONTROL_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcPowerScale() {
		return this.getDcPowerScaleChannel().value();
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
	 * Gets the Production Power in [W]. This is the power delivered by the inverter
	 * Discharge. See {@link ChannelId#PRODUCTION_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getProductionPower() {
		return this.getProductionPowerChannel().value();
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
}

	

