package io.openems.edge.predictor.profileclusteringmodel.training;

import io.openems.edge.predictor.api.common.TrainingException;

public class TrainingRunnable implements Runnable {

	private final TrainingContext trainingContext;

	public TrainingRunnable(TrainingContext trainingContext) {
		this.trainingContext = trainingContext;
	}

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
