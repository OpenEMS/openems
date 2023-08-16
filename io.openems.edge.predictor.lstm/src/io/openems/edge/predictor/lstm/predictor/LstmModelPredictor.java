package io.openems.edge.predictor.lstm.predictor;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;


public interface LstmModelPredictor extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		
		DIDWEPREDICT(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.WRITE_ONLY).persistencePriority(PersistencePriority.LOW))
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
