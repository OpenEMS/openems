package io.openems.edge.predictor.api.common;

public class PredictionException extends Exception {
	
	private static final long serialVersionUID = 42L;
	
	private final PredictionError error;

	public PredictionException(PredictionError error, String message) {
		super(message);
		this.error = error;
	}

	public PredictionException(PredictionError error, Throwable cause) {
		super(cause);
		this.error = error;
	}

	public PredictionError getError() {
		return this.error;
	}
}
