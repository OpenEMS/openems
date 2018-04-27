package io.openems.edge.ess.symmetric.readonly.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.ess.api.Ess;

@ProviderType
public interface EssSymmetricReadonly extends Ess {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		ACTIVE_POWER(new Doc().unit(Unit.WATT).text("negative values for Charge; positive for Discharge")), //
		CHARGE_ACTIVE_POWER(new Doc().unit(Unit.WATT)), //
		DISCHARGE_ACTIVE_POWER(new Doc().unit(Unit.WATT)), //
		REACTIVE_POWER(
				new Doc().unit(Unit.VOLT_AMPERE_REACTIVE).text("negative values for Charge; positive for Discharge")), //
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

	default Channel<?> getActivePower() {
		return this.channel(ChannelId.ACTIVE_POWER);
	}

	default Channel<?> getChargeActivePower() {
		return this.channel(ChannelId.CHARGE_ACTIVE_POWER);
	}

	default Channel<?> getDischargeActivePower() {
		return this.channel(ChannelId.DISCHARGE_ACTIVE_POWER);
	}

	default Channel<?> getReactivePower() {
		return this.channel(ChannelId.REACTIVE_POWER);
	}

	default Channel<?> getChargeReactivePower() {
		return this.channel(ChannelId.CHARGE_REACTIVE_POWER);
	}

	default Channel<?> getDischargeReactivePower() {
		return this.channel(ChannelId.DISCHARGE_REACTIVE_POWER);
	}
}
