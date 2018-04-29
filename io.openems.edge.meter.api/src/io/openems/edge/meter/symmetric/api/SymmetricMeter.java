package io.openems.edge.meter.symmetric.api;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.meter.api.Meter;

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
public interface SymmetricMeter extends Meter {

	public final static String POWER_DOC_TEXT = "Negative values for Consumption; positive for Production";

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/**
		 * Active Power [W].
		 * 
		 * <ul>
		 * <li>Negative value represents Consumption, i.e. power that is 'leaving the
		 * system', e.g. feed-to-grid
		 * <li>Positive value represents Production, i.e. power that is 'entering the
		 * system', e.g. buy-from-grid
		 * </ul>
		 */
		ACTIVE_POWER(new Doc().unit(Unit.WATT).text(POWER_DOC_TEXT)), //
		/**
		 * Consumption Active Power [W], derived from negative ActivePower.
		 */
		CONSUMPTION_ACTIVE_POWER(new Doc().unit(Unit.WATT)), //
		/**
		 * Production Active Power [W], derived from positive ActivePower.
		 */
		PRODUCTION_ACTIVE_POWER(new Doc().unit(Unit.WATT)), //
		/**
		 * Reactive Power [var].
		 * 
		 * <ul>
		 * <li>Negative value represents Consumption, i.e. power that is 'leaving the
		 * system', e.g. feed-to-grid
		 * <li>Positive value represents Production, i.e. power that is 'entering the
		 * system', e.g. buy-from-grid
		 * </ul>
		 */
		REACTIVE_POWER(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE).text(POWER_DOC_TEXT)), //
		/**
		 * Consumption Reactive Power [var], derived from negative ReactivePower.
		 */
		CONSUMPTION_REACTIVE_POWER(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		/**
		 * Production Reactive Power [var], derived from positive ReactivePower.
		 */
		PRODUCTION_REACTIVE_POWER(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		/**
		 * Voltage [mV]
		 */
		VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		/**
		 * Current [mA]
		 */
		CURRENT(new Doc().unit(Unit.MILLIAMPERE)); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

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
	 * Gets the Consumption Active Power in [W]. This is derived from negative
	 * 'getActivePower()' values; 0 for positive.
	 * 
	 * @return
	 */
	default Channel<Integer> getConsumptionActivePower() {
		return this.channel(ChannelId.CONSUMPTION_ACTIVE_POWER);
	}

	/**
	 * Gets the Production Active Power in [W]. This is derived from positive
	 * 'getActivePower()' values; 0 for negative.
	 * 
	 * @return
	 */
	default Channel<Integer> getProductionActivePower() {
		return this.channel(ChannelId.PRODUCTION_ACTIVE_POWER);
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
	 * Gets the Consumption Reactive Power in [var]. This is derived from negative
	 * 'getReactivePower()' values; 0 for positive.
	 * 
	 * @return
	 */
	default Channel<Integer> getConsumptionReactivePower() {
		return this.channel(ChannelId.CONSUMPTION_REACTIVE_POWER);
	}

	/**
	 * Gets the Production Reactive Power in [var]. This is derived from positive
	 * 'getReactivePower()' values; 0 for negative.
	 * 
	 * @return
	 */
	default Channel<Integer> getProductionReactivePower() {
		return this.channel(ChannelId.PRODUCTION_REACTIVE_POWER);
	}

}
