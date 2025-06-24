package io.openems.edge.predictor.lstm.train;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.common.ReadAndSaveModels;
import io.openems.edge.predictor.lstm.preprocessing.DataModification;
import io.openems.edge.predictor.lstm.validator.ValidationSeasonalityModel;
import io.openems.edge.predictor.lstm.validator.ValidationTrendModel;

public class TrainAndValidateBatch {

	private boolean earlyStoppingEnabled = false;
	private int earlyStoppingPatience = 5;
	private double bestValidationError = Double.MAX_VALUE;
	private int patienceCounter = 0;

	/**
	 * Enable early stopping to prevent overfitting.
	 * 
	 * @param enabled true to enable early stopping, false to disable
	 */
	public void setEarlyStoppingEnabled(boolean enabled) {
		this.earlyStoppingEnabled = enabled;
	}

	/**
	 * Set the number of epochs to wait for improvement before stopping training.
	 * 
	 * @param patience number of epochs with no improvement after which training
	 *                 will stop
	 */
	public void setEarlyStoppingPatience(int patience) {
		this.earlyStoppingPatience = patience;
	}

	public TrainAndValidateBatch(//
			ArrayList<Double> trainData, //
			ArrayList<OffsetDateTime> trainDate, //
			ArrayList<Double> validateData, //
			ArrayList<OffsetDateTime> validateDate, //
			HyperParameters hyperParameter) {

		/*
		 * var checkTrain = trainData.size() / hyperParameter.getBatchSize()
		 * 
		 * if ( checkTrain <= hyperParameter.getWindowSizeSeasonality() || checkTrain <=
		 * hyperParameter.getWindowSizeTrend() ) { throw new Exception; }
		 */

		var batchedData = DataModification.getDataInBatch(//
				trainData, hyperParameter.getBatchSize());
		var batchedDate = DataModification.getDateInBatch(//
				trainDate, hyperParameter.getBatchSize());

		for (int epoch = hyperParameter.getEpochTrack(); epoch < hyperParameter.getEpoch(); epoch++) {

			int k = hyperParameter.getCount();

			// Record initial validation error at start of training
			if (epoch == 0 && this.earlyStoppingEnabled) {
				// Initialize best validation error with the first validation result
				this.bestValidationError = Double.MAX_VALUE;
			}

			for (int batch = hyperParameter.getBatchTrack(); batch < hyperParameter.getBatchSize(); batch++) {

				hyperParameter.setCount(k);
				System.out.println("=====> Batch = " + hyperParameter.getBatchTrack() //
						+ "/" + hyperParameter.getBatchSize());
				System.out.println("=====> Epoch=  " + epoch //
						+ "/" + hyperParameter.getEpoch());

				MakeModel makeModels = new MakeModel();

				var trainDataTemp = batchedData.get(batch);
				var trainDateTemp = batchedDate.get(batch);

				CompletableFuture<Void> firstTaskFuture = CompletableFuture
						// Train the Seasonality model
						.supplyAsync(() -> makeModels.trainSeasonality(trainDataTemp, trainDateTemp, hyperParameter))

						// Validate this Seasonality model
						.thenAccept(untestedSeasonalityMoadels -> new ValidationSeasonalityModel().validateSeasonality(
								validateData, validateDate, untestedSeasonalityMoadels, hyperParameter));

				CompletableFuture<Void> secondTaskFuture = CompletableFuture
						// Train the trend model
						.supplyAsync(() -> makeModels.trainTrend(trainDataTemp, trainDateTemp, hyperParameter))

						// validate the trend model
						.thenAccept(untestedSeasonalityMoadels -> new ValidationTrendModel().validateTrend(validateData,
								validateDate, untestedSeasonalityMoadels, hyperParameter));

				k = k + 1;
				try {
					CompletableFuture.allOf(firstTaskFuture, secondTaskFuture).get();

					// Check if early stopping is enabled and implement the early stopping logic
					if (this.earlyStoppingEnabled) {
						// Get current validation error
						double currentValidationError = Collections.min(hyperParameter.getRmsErrorSeasonality());

						// Check if error has improved
						if (currentValidationError < this.bestValidationError) {
							// Error improved, update best error and reset patience counter
							this.bestValidationError = currentValidationError;
							this.patienceCounter = 0;
						} else {
							// Error didn't improve, increment patience counter
							this.patienceCounter++;

							// If patience exceeded, stop training
							if (this.patienceCounter >= this.earlyStoppingPatience) {
								System.out.println("Early stopping triggered - no improvement after "
										+ this.earlyStoppingPatience + " epochs");
								// Break out of the epoch loop
								epoch = hyperParameter.getEpoch(); // This will exit the epoch loop
								break; // Exit the batch loop
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				hyperParameter.setBatchTrack(batch + 1);
				hyperParameter.setCount(k);
				ReadAndSaveModels.save(hyperParameter);
			}

			hyperParameter.setBatchTrack(0);
			hyperParameter.setEpochTrack(hyperParameter.getEpochTrack() + 1);
			hyperParameter.update();
			ReadAndSaveModels.save(hyperParameter);
		}

		hyperParameter.setEpochTrack(0);
		ReadAndSaveModels.save(hyperParameter);
	}
}
