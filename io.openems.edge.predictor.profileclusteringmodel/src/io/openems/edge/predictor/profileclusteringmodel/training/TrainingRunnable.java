package io.openems.edge.predictor.profileclusteringmodel.training;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrainingRunnable implements Runnable {

	private final Logger log = LoggerFactory.getLogger(TrainingRunnable.class);
	private final TrainingContext trainingContext;

	public TrainingRunnable(TrainingContext trainingContext) {
		this.trainingContext = trainingContext;
	}

	@Override
	public void run() {
		try {
			new TrainingOrchestrator(this.trainingContext).runTraining();
		} catch (Exception e) {
			this.log.error("Cannot train profile clustering model: {}", e.getMessage(), e);
		}
	}
}
