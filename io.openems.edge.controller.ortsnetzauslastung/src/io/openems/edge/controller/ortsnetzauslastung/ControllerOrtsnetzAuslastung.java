package io.openems.edge.controller.ortsnetzauslastung;

import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ortsnetzauslastung.Types.GridStatus;
import io.openems.edge.controller.ortsnetzauslastung.Types.StorageRecommendation;

public interface ControllerOrtsnetzAuslastung extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		UNABLE_TO_SEND(Doc.of(Level.WARNING)//
				.translationKey(ControllerOrtsnetzAuslastung.class, "unableToSend")), //

		NO_COORDINATES(Doc.of(Level.WARNING)//
				.translationKey(ControllerOrtsnetzAuslastung.class, "noCoordinates")), //

		NO_GRID_METER(Doc.of(Level.WARNING)//
				.translationKey(ControllerOrtsnetzAuslastung.class, "noGridMeter")), //

		NO_VOLTAGE(Doc.of(Level.WARNING)//
				.translationKey(ControllerOrtsnetzAuslastung.class, "noVoltage")), //

		LOCATION_BLOCKED(Doc.of(Level.WARNING)//
				.translationKey(ControllerOrtsnetzAuslastung.class, "locationBlocked")), //

		REQUEST_REJECTED(Doc.of(Level.WARNING)//
				.translationKey(ControllerOrtsnetzAuslastung.class, "requestRejected")), //

		THIRD_PARTY_USAGE_NOT_ACCEPTED(Doc.of(Level.WARNING)//
				.translationKey(ControllerOrtsnetzAuslastung.class, "thirdPartyUsageNotAccepted")), //

		GRID_STATUS(Doc.of(GridStatus.values()) //
				.text("Overall grid voltage status as reported by the API")), //

		GRID_STATUS_L1(Doc.of(GridStatus.values()) //
				.text("Grid voltage status of L1 as reported by the API")), //

		GRID_STATUS_L2(Doc.of(GridStatus.values()) //
				.text("Grid voltage status of L2 as reported by the API")), //

		GRID_STATUS_L3(Doc.of(GridStatus.values()) //
				.text("Grid voltage status of L3 as reported by the API")), //

		STORAGE_RECOMMENDATION(Doc.of(StorageRecommendation.values()) //
				.text("Storage recommendation as reported by the API")), //

		REDISPATCH_ACTIVE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Redispatch measures nearby as reported by the API; undefined if unknown")), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
}
