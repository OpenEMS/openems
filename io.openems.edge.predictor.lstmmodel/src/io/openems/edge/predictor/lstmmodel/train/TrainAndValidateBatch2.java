package io.openems.edge.predictor.lstmmodel.train;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.common.ReadAndSaveModels;
import io.openems.edge.predictor.lstmmodel.preprocessing.DataModification;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.MakeModelTest;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.ValidateSeasonailtyTest;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.ValidateTrendTest;

public class TrainAndValidateBatch2 {

	public TrainAndValidateBatch2(//
			ArrayList<Double> trainData, //
			ArrayList<OffsetDateTime> trainDate, //
			ArrayList<Double> validateData, //
			ArrayList<OffsetDateTime> validateDate, //
			HyperParameters hyperParameter) {

		var batchedData = DataModification.getDataInBatch(//
				trainData, hyperParameter.getBatchSize());
		var batchedDate = DataModification.getDateInBatch(//
				trainDate, hyperParameter.getBatchSize());

		for (int epoch = hyperParameter.getEpochTrack(); epoch < hyperParameter.getEpoch(); epoch++) {

			int k = hyperParameter.getCount();

			for (int batch = hyperParameter.getBatchTrack(); batch < hyperParameter.getBatchSize(); batch++) {

				hyperParameter.setCount(k);
				System.out.println("=====> Batch = " + hyperParameter.getBatchTrack() //
						+ "/" + hyperParameter.getBatchSize());
				System.out.println("=====> Epoch=  " + epoch //
						+ "/" + hyperParameter.getEpoch());

				MakeModelTest makeModels = new MakeModelTest();

				var trainDataTemp = batchedData.get(batch);
				var trainDateTemp = batchedDate.get(batch);

				CompletableFuture<Void> firstTaskFuture = CompletableFuture

						// Train the Seasonality model
						.supplyAsync(() -> makeModels.trainSeasonality(trainDataTemp, trainDateTemp, hyperParameter))

						// Validate this Seasonality model
						.thenAccept(untestedSeasonalityMoadels -> ValidateSeasonailtyTest.validateSeasonality(
								validateData, validateDate, untestedSeasonalityMoadels, hyperParameter));

				CompletableFuture<Void> secondTaskFuture = CompletableFuture

						// Train the trend model
						.supplyAsync(() -> makeModels.trainTrend(trainDataTemp, trainDateTemp, hyperParameter))

						// validate the trend model
						.thenAccept(untestedSEasonalityMoadels -> new ValidateTrendTest().validateTrendTest(
								validateData, validateDate, untestedSEasonalityMoadels, hyperParameter));

				k = k + 1;
				try {
					CompletableFuture.allOf(firstTaskFuture, secondTaskFuture).get();
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
