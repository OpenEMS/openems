package io.openems.edge.controller.ess.fixactivepower;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.Unit.CUMULATED_SECONDS;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.LONG;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerEssFixActivePower extends Controller, OpenemsComponent {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CUMULATED_ACTIVE_TIME(Doc.of(LONG)//
				.unit(CUMULATED_SECONDS)//
				.persistencePriority(HIGH)),

		/**
		 * Configured power setpoint was limited by ESS hardware constraints.
		 */
		SETPOINT_LIMITED_BY_ESS_HARDWARE(Doc.of(Level.WARNING)//
				.translationKey(ControllerEssFixActivePower.class, "setpointLimitedByEssHardware")),

		/**
		 * Configured power setpoint was limited by global Meta constraints.
		 */
		SETPOINT_LIMITED_BY_META_LIMIT(Doc.of(Level.WARNING)//
				.translationKey(ControllerEssFixActivePower.class, "setpointLimitedByMetaLimit")),

		/**
		 * No power constraint was applied because the setpoint was blocked by a
		 * Meta-Limit only (e.g. charge-from-grid disabled), allowing other controllers
		 * to set proper constraints.
		 */
		NO_LIMIT_APPLIED(Doc.of(Level.INFO)//
				.translationKey(ControllerEssFixActivePower.class, "noLimitApplied")),

		/**
		 * Real power target after applying all constraints.
		 * 
		 * <p>
		 * The configured power is not applied directly to the Ess, but limited by the
		 * meta settings like hardware grid-buy/sell limits or general permissions for
		 * charging/discharging from/into the grid.
		 * </p>
		 */
		TARGET_AFTER_LIMITATIONS(Doc.of(INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(HIGH));

		final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
}
