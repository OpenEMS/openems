package io.openems.edge.predictor.weather.forecast;


import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface PredictorWeatherForecastModel extends  OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		PRODUCTION_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),		
		
		
		PRODUCTION_ACTIVE_POWER_PV1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),	
		
		PRODUCTION_ACTIVE_POWER_PV2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),	
		
		PRODUCTION_ACTIVE_POWER_PV3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
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
	 * Gets the Channel for {@link ChannelId#PRODUCTION_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getProductionActivePowerChannel() {
		return this.channel(ChannelId.PRODUCTION_ACTIVE_POWER);
	}

	/**
	 * Gets the State of Charge in [%], range 0..100 %. See {@link ChannelId#PRODUCTION_ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getProductionActivePower() {
		return this.getProductionActivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#PRODUCTION_ACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionActivePower(Integer value) {
		this.getProductionActivePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#PRODUCTION_ACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionActivePower(int value) {
		this.getProductionActivePowerChannel().setNextValue(value);
	}	
	
	//### PV 1
	
	/**
	 * Gets the Channel for {@link ChannelId#PRODUCTION_ACTIVE_POWER_PV1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getProductionActivePowerPv1Channel() {
		return this.channel(ChannelId.PRODUCTION_ACTIVE_POWER_PV1);
	}

	/**
	 * Gets the State of Charge in [%], range 0..100 %. See {@link ChannelId#PRODUCTION_ACTIVE_POWER_PV1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getProductionActivePowerPv1() {
		return this.getProductionActivePowerPv1Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#PRODUCTION_ACTIVE_POWER_PV1} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionActivePowerPv1(Integer value) {
		this.getProductionActivePowerPv1Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#PRODUCTION_ACTIVE_POWER_PV1} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionActivePowerPv1(int value) {
		this.getProductionActivePowerPv1Channel().setNextValue(value);
	}	
	
	//### PV 2
	
	/**
	 * Gets the Channel for {@link ChannelId#PRODUCTION_ACTIVE_POWER_PV2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getProductionActivePowerPv2Channel() {
		return this.channel(ChannelId.PRODUCTION_ACTIVE_POWER_PV2);
	}

	/**
	 * Gets the State of Charge in [%], range 0..100 %. See {@link ChannelId#PRODUCTION_ACTIVE_POWER_PV2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getProductionActivePowerPv2() {
		return this.getProductionActivePowerPv2Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#PRODUCTION_ACTIVE_POWER_PV2} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionActivePowerPv2(Integer value) {
		this.getProductionActivePowerPv2Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#PRODUCTION_ACTIVE_POWER_PV2} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionActivePowerPv2(int value) {
		this.getProductionActivePowerPv2Channel().setNextValue(value);
	}		
	
	//### PV 3
	
	/**
	 * Gets the Channel for {@link ChannelId#PRODUCTION_ACTIVE_POWER_PV3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getProductionActivePowerPv3Channel() {
		return this.channel(ChannelId.PRODUCTION_ACTIVE_POWER_PV3);
	}

	/**
	 * Gets the State of Charge in [%], range 0..100 %. See {@link ChannelId#PRODUCTION_ACTIVE_POWER_PV3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getProductionActivePowerPv3() {
		return this.getProductionActivePowerPv3Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#PRODUCTION_ACTIVE_POWER_PV3} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionActivePowerPv3(Integer value) {
		this.getProductionActivePowerPv3Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#PRODUCTION_ACTIVE_POWER_PV3} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionActivePowerPv3(int value) {
		this.getProductionActivePowerPv3Channel().setNextValue(value);
	}		

}