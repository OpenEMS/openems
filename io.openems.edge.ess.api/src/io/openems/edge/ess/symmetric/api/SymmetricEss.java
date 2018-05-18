package io.openems.edge.ess.symmetric.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.ess.power.symmetric.SymmetricPower;
import io.openems.edge.ess.symmetric.readonly.api.SymmetricEssReadonly;

@ProviderType
public interface SymmetricEss extends SymmetricEssReadonly {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/**
		 * Set Active Power
		 * 
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Writable
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_ACTIVE_POWER(new Doc().isWritable().type(OpenemsType.INTEGER).unit(Unit.WATT)
				.text("negative values for Charge; positive for Discharge")), //
		/**
		 * Set Reactive Power
		 * 
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Writable
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_REACTIVE_POWER(new Doc().isWritable().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)
				.text("negative values for Charge; positive for Discharge"));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the 'Power' class, which allows to set limitations to Active and
	 * Reactive Power.
	 * 
	 * @return
	 */
	public SymmetricPower getPower();

}
