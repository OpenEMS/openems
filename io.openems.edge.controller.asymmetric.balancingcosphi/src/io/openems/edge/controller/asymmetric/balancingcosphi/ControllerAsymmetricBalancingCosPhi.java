package io.openems.edge.controller.asymmetric.balancingcosphi;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerAsymmetricBalancingCosPhi extends Controller, OpenemsComponent {

	public static final CosPhiDirection DEFAULT_DIRECTION = CosPhiDirection.CAPACITIVE;
	public static final double DEFAULT_COS_PHI = 1d;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
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
