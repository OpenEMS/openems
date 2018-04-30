package io.openems.edge.ess.symmetric.readonly.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.ess.api.Ess;

@ProviderType
public interface SymmetricEssReadonly extends Ess {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		ACTIVE_POWER(new Doc().unit(Unit.WATT).text("negative values for Charge; positive for Discharge")), //
		CHARGE_ACTIVE_POWER(new Doc().unit(Unit.WATT)), //
		DISCHARGE_ACTIVE_POWER(new Doc().unit(Unit.WATT)), //
		REACTIVE_POWER(
				new Doc().unit(Unit.VOLT_AMPERE_REACTIVE).text("Negative values for Charge; positive for Discharge")), //
		CHARGE_REACTIVE_POWER(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		DISCHARGE_REACTIVE_POWER(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Active Power in [W]. Negative values for Charge; positive for
	 * Discharge
	 * 
	 * @return
	 */
	default Channel<Integer> getActivePower() {
		return this.channel(ChannelId.ACTIVE_POWER);
	}

	/**
	 * Gets the Charge Active Power in [W]. This is derived from negative
	 * 'getActivePower()' values; 0 for positive.
	 * 
	 * @return
	 */
	default Channel<Integer> getChargeActivePower() {
		return this.channel(ChannelId.CHARGE_ACTIVE_POWER);
	}

	/**
	 * Gets the Discharge Active Power in [W]. This is derived from positive
	 * 'getActivePower()' values; 0 for negative.
	 * 
	 * @return
	 */
	default Channel<Integer> getDischargeActivePower() {
		return this.channel(ChannelId.DISCHARGE_ACTIVE_POWER);
	}

	/**
	 * Gets the Reactive Power in [var]. Negative values for Charge; positive for
	 * Discharge
	 * 
	 * @return
	 */
	default Channel<Integer> getReactivePower() {
		return this.channel(ChannelId.REACTIVE_POWER);
	}

	/**
	 * Gets the Charge Reactive Power in [var]. This is derived from negative
	 * 'getReactivePower()' values; 0 for positive.
	 * 
	 * @return
	 */
	default Channel<Integer> getChargeReactivePower() {
		return this.channel(ChannelId.CHARGE_REACTIVE_POWER);
	}

	/**
	 * Gets the Discharge Reactive Power in [var]. This is derived from positive
	 * 'getReactivePower()' values; 0 for negative.
	 * 
	 * @return
	 */
	default Channel<Integer> getDischargeReactivePower() {
		return this.channel(ChannelId.DISCHARGE_REACTIVE_POWER);
	}
}
