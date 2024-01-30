package io.openems.edge.predictor.lstm.multithread;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import io.openems.edge.predictor.lstm.common.DataModification;
import io.openems.edge.predictor.lstm.common.DeletModels;
import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.common.Saveobject;

public class TrainInBatch {
	public TrainInBatch(ArrayList<Double> trainData, ArrayList<OffsetDateTime> trainDate,
			ArrayList<Double> validateData, ArrayList<OffsetDateTime> validateDate, HyperParameters hyperParameter) {

		int epochTrack = hyperParameter.getEpochTrack();

		ArrayList<ArrayList<Double>> batchedData = DataModification.getDataInBatch(trainData,
				hyperParameter.getBatchSize());
		ArrayList<ArrayList<OffsetDateTime>> batchedDate = DataModification.getDateInBatch(trainDate,
				hyperParameter.getBatchSize());

		for (int j = hyperParameter.getBatchTrack(); j < hyperParameter.getBatchSize(); j++) {

			int k = hyperParameter.getCount();

			for (int i = epochTrack; i < hyperParameter.getEpoch(); i++) {
				final long startTime = System.currentTimeMillis();

				System.out.println("");
				System.out.println(
						"......................................................................................................");
				System.out.println("");
				System.out.println("Batch = " + hyperParameter.getBatchTrack() + "/" + hyperParameter.getBatchSize());
				System.out.println("Epoch=  " + i + "/" + hyperParameter.getEpoch());
				CompletableFuture<Void> firstTaskFuture = CompletableFuture
						.runAsync(new MulThrTrendTrain(batchedData.get(j), batchedDate.get(j), hyperParameter));
				CompletableFuture<Void> secondTaskFuture = CompletableFuture
						.runAsync(new MulThrSeasonalityTrain(batchedData.get(j), batchedDate.get(j), hyperParameter));
				CompletableFuture<Void> thirdTaskFuture = firstTaskFuture
						.thenRun(new MulThrTrendValidate(validateData, validateDate, hyperParameter));
				CompletableFuture<Void> fourthTaskFuture = secondTaskFuture
						.thenRun(new MulThrSeasonalityValidate(validateData, validateDate, hyperParameter));
				k = k + 1;
				// System.out.println("This is k, AKA count " + k);
				try {
					CompletableFuture.allOf(thirdTaskFuture, fourthTaskFuture).get();
				} catch (Exception e) {
					e.printStackTrace();
				}
				long endTime = System.currentTimeMillis();
				long elapsedTime = endTime - startTime;
				System.out.println("Computation time: " + elapsedTime / 60000 + " minute");
				hyperParameter.setEpochTrack(i + 1);
				hyperParameter.setCount(k);

				Saveobject.save(hyperParameter);

			}
			hyperParameter.setBatchTrack(hyperParameter.getBatchTrack() + 1);
			hyperParameter.setEpochTrack(0);
			DeletModels.delet(hyperParameter);

		}

	}

}
