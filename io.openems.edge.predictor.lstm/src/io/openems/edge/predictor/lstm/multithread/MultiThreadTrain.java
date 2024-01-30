package io.openems.edge.predictor.lstm.multithread;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.common.Saveobject;

public class MultiThreadTrain {

	public MultiThreadTrain(ArrayList<Double> trainData, ArrayList<OffsetDateTime> trainDate,
			ArrayList<Double> validateData, ArrayList<OffsetDateTime> validateDate, HyperParameters hyperParameter) {
		int k = hyperParameter.getCount();
		int epochTrack = hyperParameter.getEpochTrack();

		for (int i = epochTrack; i < hyperParameter.getEpoch(); i++) {
			final long startTime = System.currentTimeMillis();

			System.out.println("");
			System.out.println("Epoch=  " + i + "/" + hyperParameter.getEpoch());
			System.out.println("");
			CompletableFuture<Void> firstTaskFuture = CompletableFuture
					.runAsync(new MulThrTrendTrain(trainData, trainDate, hyperParameter));
			CompletableFuture<Void> secondTaskFuture = CompletableFuture
					.runAsync(new MulThrSeasonalityTrain(trainData, trainDate, hyperParameter));
			CompletableFuture<Void> thirdTaskFuture = firstTaskFuture
					.thenRun(new MulThrTrendValidate(validateData, validateDate, hyperParameter));
			CompletableFuture<Void> fourthTaskFuture = secondTaskFuture
					.thenRun(new MulThrSeasonalityValidate(validateData, validateDate, hyperParameter));
			k = k + 1;
			System.out.println("This is k, AKA count " + k);
			try {
				CompletableFuture.allOf(thirdTaskFuture, fourthTaskFuture).get();
			} catch (Exception e) {
				e.printStackTrace();
			}
			long endTime = System.currentTimeMillis();
			long elapsedTime = endTime - startTime;
			System.out.println("");
			System.out.println("Computation time: " + elapsedTime / 60000 + " minute");
			hyperParameter.setEpochTrack(i + 1);
			hyperParameter.setCount(k);
			Saveobject.save(hyperParameter);

		}
		hyperParameter.setEpochTrack(0);

	}

}
