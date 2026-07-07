package io.openems.edge.predictor.api.common;

public class TrainingException extends Exception {

	private static final long serialVersionUID = 42L;
	
	private final TrainingError error;

	public TrainingException(TrainingError error, String message) {
		super(message);
		this.error = error;
	}

	public TrainingException(TrainingError error, Throwable cause) {
		super(cause);
		this.error = error;
	}

	public TrainingError getError() {
		return this.error;
	}
}
