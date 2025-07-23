package io.openems.edge.heat.mypv.acthor9s;

import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface HeatMyPvAcThor9s extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		BOOST_ACTIVE(Doc.of(Level.FAULT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Boost mode overrides the controls " //
						+ "| Please disable boost mode in the MyPv app ")); //

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
