package io.openems.edge.predictor.production.linearmodel.training;

import io.openems.edge.predictor.api.common.TrainingException;

public record TrainingRunnable(TrainingContext trainingContext) implements Runnable {

	@Override
	public void run() {
		try {
			var modelBundle = new TrainingOrchestrator(this.trainingContext)//
					.runTraining();
			this.trainingContext.callback()//
					.onTrainingSuccess(modelBundle);
		} catch (TrainingException e) {
			this.trainingContext.callback()//
					.onTrainingError(e.getError(), e.getMessage());
		} catch (Exception e) {
			this.trainingContext.callback()//
					.onTrainingError(TrainingError.UNKNOWN, e.getMessage());
		}
	}
}