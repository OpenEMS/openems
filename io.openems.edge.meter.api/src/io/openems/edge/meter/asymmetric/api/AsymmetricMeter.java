package io.openems.edge.meter.asymmetric.api;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.meter.api.Meter;

/**
 * Represents an Asymmetric Meter.
 * 
 * - Negative ActivePowerL1/L2/L3 and ConsumptionActivePowerL1/L2/L3 represent
 * Consumption, i.e. power that is 'leaving the system', e.g. feed-to-grid
 * 
 * - Positive ActivePowerL1/L2/L3 and ProductionActivePowerL1/L2/L3 represent
 * Production, i.e. power that is 'entering the system', e.g. buy-from-grid
 * 
 * @author stefan.feilmeier
 *
 */
public interface AsymmetricMeter extends Meter {

	public final static String POWER_DOC_TEXT = "Negative values for Consumption; positive for Production";

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/**
		 * Active Power on L1 [W].
		 * 
		 * <ul>
		 * <li>Negative value represents Consumption, i.e. power that is 'leaving the
		 * system', e.g. feed-to-grid
		 * <li>Positive value represents Production, i.e. power that is 'entering the
		 * system', e.g. buy-from-grid
		 * </ul>
		 */
		ACTIVE_POWER_L1(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT).text(POWER_DOC_TEXT)), //
		/**
		 * Active Power on L2 [W].
		 * 
		 * <ul>
		 * <li>Negative value represents Consumption, i.e. power that is 'leaving the
		 * system', e.g. feed-to-grid
		 * <li>Positive value represents Production, i.e. power that is 'entering the
		 * system', e.g. buy-from-grid
		 * </ul>
		 */
		ACTIVE_POWER_L2(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT).text(POWER_DOC_TEXT)), //
		/**
		 * Active Power on L3 [W].
		 * 
		 * <ul>
		 * <li>Negative value represents Consumption, i.e. power that is 'leaving the
		 * system', e.g. feed-to-grid
		 * <li>Positive value represents Production, i.e. power that is 'entering the
		 * system', e.g. buy-from-grid
		 * </ul>
		 */
		ACTIVE_POWER_L3(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT).text(POWER_DOC_TEXT)), //
		// TODO: derive from ACTIVE_POWER_L1
		/**
		 * Consumption Active Power on L1 [W], derived from negative ActivePowerL1.
		 */
		CONSUMPTION_ACTIVE_POWER_L1(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)), //
		/**
		 * Consumption Active Power on L2 [W], derived from negative ActivePowerL2.
		 */
		CONSUMPTION_ACTIVE_POWER_L2(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)), //
		/**
		 * Consumption Active Power on L3 [W], derived from negative ActivePowerL3.
		 */
		CONSUMPTION_ACTIVE_POWER_L3(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)), //
		/**
		 * Production Active Power on L1 [W], derived from positive ActivePowerL1.
		 */
		PRODUCTION_ACTIVE_POWER_L1(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)), //
		/**
		 * Production Active Power on L2 [W], derived from positive ActivePowerL2.
		 */
		PRODUCTION_ACTIVE_POWER_L2(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)), //
		/**
		 * Production Active Power on L3 [W], derived from positive ActivePowerL3.
		 */
		PRODUCTION_ACTIVE_POWER_L3(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)), //
		/**
		 * Reactive Power on L1 [var].
		 * 
		 * <ul>
		 * <li>Negative value represents Consumption, i.e. power that is 'leaving the
		 * system', e.g. feed-to-grid
		 * <li>Positive value represents Production, i.e. power that is 'entering the
		 * system', e.g. buy-from-grid
		 * </ul>
		 */
		REACTIVE_POWER_L1(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE).text(POWER_DOC_TEXT)), //
		/**
		 * Reactive Power on L2 [var].
		 * 
		 * <ul>
		 * <li>Negative value represents Consumption, i.e. power that is 'leaving the
		 * system', e.g. feed-to-grid
		 * <li>Positive value represents Production, i.e. power that is 'entering the
		 * system', e.g. buy-from-grid
		 * </ul>
		 */
		REACTIVE_POWER_L2(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE).text(POWER_DOC_TEXT)), //
		/**
		 * Reactive Power on L3 [var].
		 * 
		 * <ul>
		 * <li>Negative value represents Consumption, i.e. power that is 'leaving the
		 * system', e.g. feed-to-grid
		 * <li>Positive value represents Production, i.e. power that is 'entering the
		 * system', e.g. buy-from-grid
		 * </ul>
		 */
		REACTIVE_POWER_L3(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE).text(POWER_DOC_TEXT)), //
		/**
		 * Consumption Reactive Power on L1 [var], derived from negative
		 * ReactivePowerL1.
		 */
		CONSUMPTION_REACTIVE_POWER_L1(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)), //
		/**
		 * Consumption Reactive Power on L2 varW], derived from negative
		 * ReactivePowerL2.
		 */
		CONSUMPTION_REACTIVE_POWER_L2(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)), //
		/**
		 * Consumption Reactive Power on L3 [var], derived from negative
		 * ReactivePowerL3.
		 */
		CONSUMPTION_REACTIVE_POWER_L3(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)), //
		/**
		 * Production Reactive Power on L1 [var], derived from positive ReactivePowerL1.
		 */
		PRODUCTION_REACTIVE_POWER_L1(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)), //
		/**
		 * Production Reactive Power on L2 [var], derived from positive ReactivePowerL2.
		 */
		PRODUCTION_REACTIVE_POWER_L2(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)), //
		/**
		 * Production Reactive Power on L3 [var], derived from positive ReactivePowerL3.
		 */
		PRODUCTION_REACTIVE_POWER_L3(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)), //
		/**
		 * Voltage on L1 [mV]
		 */
		VOLTAGE_L1(new Doc().type(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		/**
		 * Voltage on L2 [mV]
		 */
		VOLTAGE_L2(new Doc().type(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		/**
		 * Voltage on L3 [mV]
		 */
		VOLTAGE_L3(new Doc().type(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		/**
		 * Current on L1 [mA]
		 */
		CURRENT_L1(new Doc().type(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
		/**
		 * Current on L2 [mA]
		 */
		CURRENT_L2(new Doc().type(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
		/**
		 * Current on L3 [mA]
		 */
		CURRENT_L3(new Doc().type(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Active Power for L1 in [W]. Negative values for Consumption;
	 * positive for Production
	 * 
	 * @return
	 */
	default Channel<Integer> getActivePowerL1() {
		return this.channel(ChannelId.ACTIVE_POWER_L1);
	}

	/**
	 * Gets the Active Power for L2 in [W]. Negative values for Consumption;
	 * positive for Production
	 * 
	 * @return
	 */
	default Channel<Integer> getActivePowerL2() {
		return this.channel(ChannelId.ACTIVE_POWER_L2);
	}

	/**
	 * Gets the Active Power for L3 in [W]. Negative values for Consumption;
	 * positive for Production
	 * 
	 * @return
	 */
	default Channel<Integer> getActivePowerL3() {
		return this.channel(ChannelId.ACTIVE_POWER_L3);
	}

	/**
	 * Gets the Consumption Active Power for L1 in [W]. This is derived from
	 * negative 'getActivePowerL1()' values; 0 for positive.
	 * 
	 * @return
	 */
	default Channel<Integer> getConsumptionActivePowerL1() {
		return this.channel(ChannelId.CONSUMPTION_ACTIVE_POWER_L1);
	}

	/**
	 * Gets the Consumption Active Power for L2 in [W]. This is derived from
	 * negative 'getActivePowerL2()' values; 0 for positive.
	 * 
	 * @return
	 */
	default Channel<Integer> getConsumptionActivePowerL2() {
		return this.channel(ChannelId.CONSUMPTION_ACTIVE_POWER_L2);
	}

	/**
	 * Gets the Consumption Active Power for L3 in [W]. This is derived from
	 * negative 'getActivePowerL3()' values; 0 for positive.
	 * 
	 * @return
	 */
	default Channel<Integer> getConsumptionActivePowerL3() {
		return this.channel(ChannelId.CONSUMPTION_ACTIVE_POWER_L3);
	}

	/**
	 * Gets the Production Active Power for L1 in [W]. This is derived from positive
	 * 'getActivePowerL1()' values; 0 for negative.
	 * 
	 * @return
	 */
	default Channel<Integer> getProductionActivePowerL1() {
		return this.channel(ChannelId.PRODUCTION_ACTIVE_POWER_L1);
	}

	/**
	 * Gets the Production Active Power for L2 in [W]. This is derived from positive
	 * 'getActivePowerL2()' values; 0 for negative.
	 * 
	 * @return
	 */
	default Channel<Integer> getProductionActivePowerL2() {
		return this.channel(ChannelId.PRODUCTION_ACTIVE_POWER_L2);
	}

	/**
	 * Gets the Production Active Power for L3 in [W]. This is derived from positive
	 * 'getActivePowerL3()' values; 0 for negative.
	 * 
	 * @return
	 */
	default Channel<Integer> getProductionActivePowerL3() {
		return this.channel(ChannelId.PRODUCTION_ACTIVE_POWER_L3);
	}

	/**
	 * Gets the Reactive Power for L1 in [var]. Negative values for Consumption;
	 * positive for Production.
	 * 
	 * @return
	 */
	default Channel<Integer> getReactivePowerL1() {
		return this.channel(ChannelId.REACTIVE_POWER_L1);
	}

	/**
	 * Gets the Reactive Power for L2 in [var]. Negative values for Consumption;
	 * positive for Production.
	 * 
	 * @return
	 */
	default Channel<Integer> getReactivePowerL2() {
		return this.channel(ChannelId.REACTIVE_POWER_L2);
	}

	/**
	 * Gets the Reactive Power for L3 in [var]. Negative values for Consumption;
	 * positive for Production.
	 * 
	 * @return
	 */
	default Channel<Integer> getReactivePowerL3() {
		return this.channel(ChannelId.REACTIVE_POWER_L3);
	}

	/**
	 * Gets the Consumption Reactive Power for L1 in [var]. This is derived from
	 * negative 'getReactivePowerL2()' values; 0 for positive.
	 * 
	 * @return
	 */
	default Channel<Integer> getConsumptionReactivePowerL1() {
		return this.channel(ChannelId.CONSUMPTION_REACTIVE_POWER_L1);
	}

	/**
	 * Gets the Consumption Reactive Power for L2 in [var]. This is derived from
	 * negative 'getReactivePowerL2()' values; 0 for positive.
	 * 
	 * @return
	 */
	default Channel<Integer> getConsumptionReactivePowerL2() {
		return this.channel(ChannelId.CONSUMPTION_REACTIVE_POWER_L2);
	}

	/**
	 * Gets the Consumption Reactive Power for L3 in [var]. This is derived from
	 * negative 'getReactivePowerL3()' values; 0 for positive.
	 * 
	 * @return
	 */
	default Channel<Integer> getConsumptionReactivePowerL3() {
		return this.channel(ChannelId.CONSUMPTION_REACTIVE_POWER_L3);
	}

	/**
	 * Gets the Production Reactive Power for L1 in [var]. This is derived from
	 * positive 'getReactivePowerL1()' values; 0 for negative.
	 * 
	 * @return
	 */
	default Channel<Integer> getProductionReactivePowerL1() {
		return this.channel(ChannelId.PRODUCTION_REACTIVE_POWER_L1);
	}

	/**
	 * Gets the Production Reactive Power for L2 in [var]. This is derived from
	 * positive 'getReactivePowerL2()' values; 0 for negative.
	 * 
	 * @return
	 */
	default Channel<Integer> getProductionReactivePowerL2() {
		return this.channel(ChannelId.PRODUCTION_REACTIVE_POWER_L2);
	}

	/**
	 * Gets the Production Reactive Power for L3 in [var]. This is derived from
	 * positive 'getReactivePowerL3()' values; 0 for negative.
	 * 
	 * @return
	 */
	default Channel<Integer> getProductionReactivePowerL3() {
		return this.channel(ChannelId.PRODUCTION_REACTIVE_POWER_L3);
	}

}
