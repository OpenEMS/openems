package io.openems.edge.predictor.persistencemodel;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;

public enum PredictorChannelId implements io.openems.edge.common.channel.ChannelId {
	UNABLE_TO_PREDICT(Doc.of(Level.FAULT));

	private final Doc doc;

	private PredictorChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}