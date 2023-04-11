package io.openems.edge.solaredge.gridmeter;


import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;



public interface SolaredgeGridmeter extends OpenemsComponent {
	
	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Grid-Mode.
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Integer/Enum
		 * <li>Range: 0=Undefined, 1=On-Grid, 2=Off-Grid
		 * </ul>
		 */
		POWER(Doc.of(OpenemsType.INTEGER) //
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
		POWER_L1(Doc.of(OpenemsType.INTEGER) //
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
		POWER_L2(Doc.of(OpenemsType.INTEGER) //
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
		POWER_L3(Doc.of(OpenemsType.INTEGER) //
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
		POWER_SCALE(Doc.of(OpenemsType.INTEGER) //
				.persistencePriority(PersistencePriority.HIGH)),	

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
	 * DC Power Channel {@link ChannelId#POWER_DC_SCALE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getPower() {
		return this.getPowerChannel().value();
	}

	//######################
	/**
	 * Gets the Channel for {@link ChannelId#POWER_DC}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getPowerChannel() {
		return this.channel(ChannelId.POWER);
	}	
	
	
	/**
	 * DC Power Channel {@link ChannelId#POWER_DC_SCALE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getPowerL1() {
		return this.getPowerL1Channel().value();
	}

	//######################
	/**
	 * Gets the Channel for {@link ChannelId#POWER_DC}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getPowerL1Channel() {
		return this.channel(ChannelId.POWER_L1);
	}	
	
	/**
	 * DC Power Channel {@link ChannelId#POWER_DC_SCALE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getPowerL2() {
		return this.getPowerL2Channel().value();
	}

	//######################
	/**
	 * Gets the Channel for {@link ChannelId#POWER_DC}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getPowerL2Channel() {
		return this.channel(ChannelId.POWER_L2);
	}	
	
	
	/**
	 * DC Power Channel {@link ChannelId#POWER_DC_SCALE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getPowerL3() {
		return this.getPowerL3Channel().value();
	}

	//######################
	/**
	 * Gets the Channel for {@link ChannelId#POWER_DC}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getPowerL3Channel() {
		return this.channel(ChannelId.POWER_L3);
	}	
	
	
	/**
	 * Is the Energy Storage System On-Grid? See {@link ChannelId#CONTROL_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getPowerScale() {
		return this.getPowerScaleChannel().value();
	}

	//######################
	/**
	 * Gets the Channel for {@link ChannelId#CONTROL_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getPowerScaleChannel() {
		return this.channel(ChannelId.POWER_SCALE);
	}		

}	
	
	


