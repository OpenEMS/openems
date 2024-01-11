package io.openems.edge.predictor.lstm.muliithread;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import io.openems.edge.predictor.lstm.common.HyperParameters;

public class MultiThreadTrain {

	public MultiThreadTrain(ArrayList<Double> trainData, ArrayList<OffsetDateTime> trainDate,
			ArrayList<Double> validateData, ArrayList<OffsetDateTime> validateDate, HyperParameters hyperParameter) {

		for (int i = 0; i < hyperParameter.getEpoch(); i++) {
			long startTime = System.currentTimeMillis();
			System.out.println("Epoch=  " + i + "/" + hyperParameter.getEpoch());

			CompletableFuture<Void> firstTaskFuture = CompletableFuture
					.runAsync(new MulThrTrendTrain(trainData, trainDate, hyperParameter));
			CompletableFuture<Void> secondTaskFuture = CompletableFuture
					.runAsync(new MulThrSeasonalityTrain(trainData, trainDate, hyperParameter));
			CompletableFuture<Void> thirdTaskFuture = firstTaskFuture
					.thenRun(new MulThrTrendValidate(validateData, validateDate, hyperParameter));

			CompletableFuture<Void> fourthTaskFuture = secondTaskFuture
					.thenRun(new MulThrSeasonalityValidate(validateData, validateDate, hyperParameter));
			

			try {
				CompletableFuture.allOf(thirdTaskFuture, fourthTaskFuture).get();
			} catch (Exception e) {
				e.printStackTrace();
			}
			 long endTime = System.currentTimeMillis();
			 long elapsedTime = endTime - startTime;
		        System.out.println("Computation time: " + elapsedTime/60000 + " minute");

		}
	}

}
