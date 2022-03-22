package io.openems.edge.ess.dccharger.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.IntUtils;
import io.openems.common.utils.IntUtils.Round;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.ess.api.SymmetricEss;

@ProviderType
public interface EssDcCharger extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Maximum Ever Actual Power
		 *
		 * <ul>
		 * <li>Interface: Ess DC Charger
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive or '0'
		 * <li>Implementation Note: value is automatically derived from ACTUAL_POWER
		 * </ul>
		 */
		MAX_ACTUAL_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Actual Power
		 *
		 * <ul>
		 * <li>Interface: Ess DC Charger
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive
		 * </ul>
		 */
		ACTUAL_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.onInit(channel -> {
					channel.onSetNextValue(value -> {
						/*
						 * Fill Max Actual Power channel
						 */
						if (value.asOptional().isPresent()) {
							int newValue = (Integer) value.get();
							Channel<Integer> maxActualPowerChannel = channel.getComponent()
									.channel(ChannelId.MAX_ACTUAL_POWER);
							int maxActualPower = maxActualPowerChannel.value().orElse(0);
							int maxNextActualPower = maxActualPowerChannel.getNextValue().orElse(0);
							if (newValue > Math.max(maxActualPower, maxNextActualPower)) {
								// avoid getting called too often -> round to 100
								newValue = IntUtils.roundToPrecision(newValue, Round.AWAY_FROM_ZERO, 100);
								maxActualPowerChannel.setNextValue(newValue);
							}
						}
					});
				})),
		/**
		 * Actual Energy
		 *
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		ACTUAL_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)); //

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
	 * Gets the Channel for {@link ChannelId#MAX_ACTUAL_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxActualPowerChannel() {
		return this.channel(ChannelId.MAX_ACTUAL_POWER);
	}

	/**
	 * Gets the Maximum Ever Actual Power in [W]. See
	 * {@link ChannelId#MAX_ACTUAL_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxActualPower() {
		return this.getMaxActualPowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_ACTUAL_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxActualPower(Integer value) {
		this.getMaxActualPowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_ACTUAL_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxActualPower(int value) {
		this.getMaxActualPowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTUAL_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getActualPowerChannel() {
		return this.channel(ChannelId.ACTUAL_POWER);
	}

	/**
	 * Gets the Actual Power in [W]. See {@link ChannelId#ACTUAL_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getActualPower() {
		return this.getActualPowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ACTUAL_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActualPower(Integer value) {
		this.getActualPowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ACTUAL_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActualPower(int value) {
		this.getActualPowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTUAL_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getActualEnergyChannel() {
		return this.channel(ChannelId.ACTUAL_ENERGY);
	}

	/**
	 * Gets the Actual Energy in [Wh]. See {@link ChannelId#ACTUAL_ENERGY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getActualEnergy() {
		return this.getActualEnergyChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ACTUAL_ENERGY}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActualEnergy(Long value) {
		this.getActualEnergyChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ACTUAL_ENERGY}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActualEnergy(long value) {
		this.getActualEnergyChannel().setNextValue(value);
	}

	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(SymmetricEss.class, accessMode, 100) //
				.channel(0, ChannelId.ACTUAL_POWER, ModbusType.FLOAT32) //
				.channel(2, ChannelId.ACTUAL_ENERGY, ModbusType.FLOAT64) //
				.build();
	}
}
