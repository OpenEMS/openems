package io.openems.edge.core.meta;

public enum GridFeedInLimitationType {
	NO_LIMITATION(io.openems.edge.common.meta.GridFeedInLimitationType.NO_LIMITATION), //
	DYNAMIC_LIMITATION(io.openems.edge.common.meta.GridFeedInLimitationType.DYNAMIC_LIMITATION), //
	; //

	final io.openems.edge.common.meta.GridFeedInLimitationType gridFeedInLimitationType;

	GridFeedInLimitationType(io.openems.edge.common.meta.GridFeedInLimitationType gridFeedInLimitationType) {
		this.gridFeedInLimitationType = gridFeedInLimitationType;
	}

	public io.openems.edge.common.meta.GridFeedInLimitationType getGridFeedInLimitationType() {
		return this.gridFeedInLimitationType;
	}
}
