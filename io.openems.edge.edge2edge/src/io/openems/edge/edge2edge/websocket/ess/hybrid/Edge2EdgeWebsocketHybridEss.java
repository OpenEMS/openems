package io.openems.edge.edge2edge.websocket.ess.hybrid;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface Edge2EdgeWebsocketHybridEss extends OpenemsComponent {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Actual PV Production Power from all DC-Chargers combined. Fallback idea for a
		 * later iteration: derive an estimate from AC active power and DC discharge
		 * power if the remote Hybrid-ESS does not expose a real PV value.
		 *
		 * <ul>
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: >= 0 (production only)
		 * </ul>
		 */
		PV_PRODUCTION(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Actual PV production power (sum of all EssDcCharger.ACTUAL_POWER)")),

		/**
		 * Surplus Power if battery is full. Needs to be curtailed or fed to grid.
		 *
		 * <ul>
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: >= 0 or null
		 * </ul>
		 */
		SURPLUS_POWER_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Surplus power when battery is full"));

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
}
