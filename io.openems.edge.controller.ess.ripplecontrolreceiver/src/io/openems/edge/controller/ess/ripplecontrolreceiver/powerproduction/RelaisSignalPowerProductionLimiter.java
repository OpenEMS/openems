package io.openems.edge.controller.ess.ripplecontrolreceiver.powerproduction;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.controller.ess.ripplecontrolreceiver.EssRestrictionLevel;
import io.openems.edge.controller.ess.ripplecontrolreceiver.PowerProductionLimiterComponent;

import static io.openems.common.channel.PersistencePriority.HIGH;

public interface RelaisSignalPowerProductionLimiter extends PowerProductionLimiterComponent {
	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		RESTRICTION_MODE(Doc.of(EssRestrictionLevel.values())//
				.persistencePriority(HIGH)), //

		UPDATE_FAILURE(Doc.of(Level.WARNING));

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Represents the current {@link EssRestrictionLevel}.
	 *
	 * <p>
	 * This is determined by the external ripple control signals.
	 * </p>
	 *
	 * @return the current restriction level.
	 */
	public default EssRestrictionLevel getRestrictionLevel() {
		return this.channel(ChannelId.RESTRICTION_MODE).value().asEnum();
	}
}
