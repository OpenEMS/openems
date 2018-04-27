package io.openems.edge.ess.symmetric.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.ess.power.symmetric.PEqualLimitation;
import io.openems.edge.ess.power.symmetric.SymmetricPower;
import io.openems.edge.ess.symmetric.readonly.api.EssSymmetricReadonly;

@ProviderType
public interface EssSymmetric extends EssSymmetricReadonly {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		SET_ACTIVE_POWER(new Doc().unit(Unit.WATT).text("negative values for Charge; positive for Discharge")), //
		SET_REACTIVE_POWER(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	public SymmetricPower getPower();

	/**
	 * Set the ActivePower
	 * 
	 * @param value
	 * 
	 *            <pre>
	 *            < 0 (negative) = Charge
	 *            > 0 (positive) = Discharge
	 *            </pre>
	 * 
	 * @throws OpenemsException
	 */
	default void setActivePowerEqual(int value) throws OpenemsException {
		SymmetricPower power = this.getPower();
		PEqualLimitation limit = new PEqualLimitation(power);
		limit.setP(value);
		power.applyLimitation(limit);
		// TODO: add set... channels for each power limitation
	}
}
