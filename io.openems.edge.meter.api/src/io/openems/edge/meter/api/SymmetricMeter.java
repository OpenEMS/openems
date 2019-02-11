package io.openems.edge.meter.api;

import io.openems.common.OpenemsConstants;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.IntUtils;
import io.openems.common.utils.IntUtils.Round;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Represents a Symmetric Meter.
 * 
 * - Negative ActivePower and ConsumptionActivePower represent Consumption, i.e.
 * power that is 'leaving the system', e.g. feed-to-grid
 * 
 * - Positive ActivePower and ProductionActivePower represent Production, i.e.
 * power that is 'entering the system', e.g. buy-from-grid
 * 
 * @author stefan.feilmeier
 *
 */
public interface SymmetricMeter extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/**
		 * Frequency
		 * 
		 * <ul>
		 * <li>Interface: Meter Symmetric
		 * <li>Type: Integer
		 * <li>Unit: mHz
		 * <li>Range: only positive values
		 * </ul>
		 */
		FREQUENCY(new Doc().type(OpenemsType.INTEGER).unit(Unit.MILLIHERTZ)), //
		/**
		 * Minimum Ever Active Power
		 * 
		 * <ul>
		 * <li>Interface: Meter Symmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative or '0'
		 * <li>Implementation Note: value is automatically derived from ACTIVE_POWER
		 * </ul>
		 */
		MIN_ACTIVE_POWER(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)), //
		/**
		 * Maximum Ever Active Power
		 * 
		 * <ul>
		 * <li>Interface: Meter Symmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive or '0'
		 * <li>Implementation Note: value is automatically derived from ACTIVE_POWER
		 * </ul>
		 */
		MAX_ACTIVE_POWER(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)), //
		/**
		 * Active Power
		 * 
		 * <ul>
		 * <li>Interface: Meter Symmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		ACTIVE_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text(OpenemsConstants.POWER_DOC_TEXT) //
				.onInit(channel -> {
					channel.onSetNextValue(value -> {
						/*
						 * Fill Min/Max Active Power channels
						 */
						if (value.asOptional().isPresent()) {
							int newValue = (Integer) value.get();
							{
								Channel<Integer> minActivePowerChannel = channel.getComponent()
										.channel(ChannelId.MIN_ACTIVE_POWER);
								int minActivePower = minActivePowerChannel.value().orElse(0);
								int minNextActivePower = minActivePowerChannel.getNextValue().orElse(0);
								if (newValue < Math.min(minActivePower, minNextActivePower)) {
									// avoid getting called too often -> round to 100
									newValue = IntUtils.roundToPrecision(newValue, Round.TOWARDS_ZERO, 100);
									minActivePowerChannel.setNextValue(newValue);
								}
							}
							{
								Channel<Integer> maxActivePowerChannel = channel.getComponent()
										.channel(ChannelId.MAX_ACTIVE_POWER);
								int maxActivePower = maxActivePowerChannel.value().orElse(0);
								int maxNextActivePower = maxActivePowerChannel.getNextValue().orElse(0);
								if (newValue > Math.max(maxActivePower, maxNextActivePower)) {
									// avoid getting called too often -> round to 100
									newValue = IntUtils.roundToPrecision(newValue, Round.AWAY_FROM_ZERO, 100);
									maxActivePowerChannel.setNextValue(newValue);
								}
							}
						}
					});
				})), //
		/**
		 * Reactive Power
		 * 
		 * <ul>
		 * <li>Interface: Meter Symmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		REACTIVE_POWER(new Doc().type(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.text(OpenemsConstants.POWER_DOC_TEXT)), //
		/**
		 * Active Production Energy
		 * 
		 * <ul>
		 * <li>Interface: Meter Symmetric
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		ACTIVE_PRODUCTION_ENERGY(new Doc() //
				.type(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)),
		/**
		 * Active Consumption Energy
		 * 
		 * <ul>
		 * <li>Interface: Meter Symmetric
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		ACTIVE_CONSUMPTION_ENERGY(new Doc() //
				.type(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)),
		/**
		 * Voltage
		 * 
		 * <ul>
		 * <li>Interface: Meter Symmetric
		 * <li>Type: Integer
		 * <li>Unit: mV
		 * </ul>
		 */
		VOLTAGE(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		/**
		 * Current
		 * 
		 * <ul>
		 * <li>Interface: Meter Symmetric
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * </ul>
		 */
		CURRENT(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the type of this Meter
	 * 
	 * @return
	 */
	MeterType getMeterType();

	/**
	 * Gets the Active Power in [W]. Negative values for Consumption; positive for
	 * Production
	 * 
	 * @return
	 */
	default Channel<Integer> getActivePower() {
		return this.channel(ChannelId.ACTIVE_POWER);
	}

	/**
	 * Gets the Reactive Power in [var]. Negative values for Consumption; positive
	 * for Production.
	 * 
	 * @return
	 */
	default Channel<Integer> getReactivePower() {
		return this.channel(ChannelId.REACTIVE_POWER);
	}

	/**
	 * Gets the Production Active Energy in [Wh]. This relates to positive
	 * ACTIVE_POWER.
	 * 
	 * @return
	 */
	default Channel<Integer> getActiveProductionEnergy() {
		return this.channel(ChannelId.ACTIVE_PRODUCTION_ENERGY);
	}

	/**
	 * Gets the Frequency in [mHz]. FREQUENCY
	 * 
	 * @return
	 */
	default Channel<Integer> getFrequency() {
		return this.channel(ChannelId.FREQUENCY);
	}

	/**
	 * Gets the Voltage in [mV].
	 * 
	 * @return
	 */

	default Channel<Integer> getVoltage() {
		return this.channel(ChannelId.VOLTAGE);
	}

	/**
	 * Gets the Consumption Active Energy in [Wh]. This relates to negative
	 * ACTIVE_POWER.
	 * 
	 * @return
	 */
	default Channel<Integer> getActiveConsumptionEnergy() {
		return this.channel(ChannelId.ACTIVE_CONSUMPTION_ENERGY);
	}

	/**
	 * Gets the Minimum Ever Active Power.
	 * 
	 * @return
	 */
	default Channel<Integer> getMinActivePower() {
		return this.channel(ChannelId.MIN_ACTIVE_POWER);
	}

	/**
	 * Gets the Maximum Ever Active Power.
	 * 
	 * @return
	 */
	default Channel<Integer> getMaxActivePower() {
		return this.channel(ChannelId.MAX_ACTIVE_POWER);
	}

	/**
	 * Internal helper method to handle storing Min/MaxActivePower in config
	 * properties 'minActivePower' and 'maxActivePower'
	 * 
	 * @param cm
	 * @param servicePid
	 * @param minActivePowerConfig
	 * @param maxActivePowerConfig
	 */
	default void _initializeMinMaxActivePower(ConfigurationAdmin cm, String servicePid, int minActivePowerConfig,
			int maxActivePowerConfig) {
		/*
		 * Update min/max active power channels
		 */
		this.getMinActivePower().setNextValue(minActivePowerConfig);
		this.getMaxActivePower().setNextValue(maxActivePowerConfig);
		// TODO: use a "StorageChannel" service for this; the following was never
		// properly working
//		this.getMinActivePower().onChange(value -> {
//			if ((Float)value.get() != (float) minActivePowerConfig) {
//				OpenemsComponent.updateConfigurationProperty(cm, servicePid, "minActivePower", ((Float) value.get()).intValue());
//			}
//		});
//		this.getMaxActivePower().onChange(value -> {
//			if ((Float) value.get() != (float) maxActivePowerConfig) {
//				OpenemsComponent.updateConfigurationProperty(cm, servicePid, "maxActivePower", ((Float) value.get()).intValue());
//			}
//		});
	}
}
