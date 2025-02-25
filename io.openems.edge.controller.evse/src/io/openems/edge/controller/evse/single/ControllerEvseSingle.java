package io.openems.edge.controller.evse.single;

import static io.openems.common.channel.Unit.WATT_HOURS;
import static io.openems.common.types.OpenemsType.INTEGER;

import com.google.common.collect.ImmutableList;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint.ApplyCharge;
import io.openems.edge.evse.api.chargepoint.Profile;

public interface ControllerEvseSingle extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SESSON_ENERGY(Doc.of(INTEGER) //
				.unit(WATT_HOURS));

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
	 * Gets the Controller {@link Params}.
	 * 
	 * @return the {@link Params}
	 */
	public Params getParams();

	/**
	 * Apply an {@link ApplyCharge} and optionally {@link Profile.Command}s.
	 * 
	 * @param applyCharge     the {@link ApplyCharge}
	 * @param profileCommands the {@link Profile.Command}s
	 */
	public void apply(ApplyCharge applyCharge, ImmutableList<Profile.Command> profileCommands);
}
