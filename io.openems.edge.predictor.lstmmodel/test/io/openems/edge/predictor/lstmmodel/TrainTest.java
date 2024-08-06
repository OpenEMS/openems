//package io.openems.edge.predictor.lstm;
//
//import java.time.OffsetDateTime;
//import java.time.ZoneOffset;
//import java.util.ArrayList;
//
//import org.junit.Test;
//
//import io.openems.edge.predictor.lstm.common.HyperParameters;
//import io.openems.edge.predictor.lstm.train.TrainAndValidateBatch;
//
//public class TrainTest {
//	@Test
//
//	public void trainTest() {
//		// generating training data
//		int j = 0;
//		int number = 288 * 300;
//		HyperParameters hyperParameters = HyperParameters.getInstance();
//		hyperParameters.setModelName("Junit");
//		hyperParameters.setGdIterration(200);
//
//		ArrayList<Double> trainingData = new ArrayList<Double>();
//		ArrayList<OffsetDateTime> trainingDate = new ArrayList<OffsetDateTime>();
//		long interval = 5;
//		// generating 10 data points
//		OffsetDateTime startingDate = OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(1));
//		for (int i = 0; i < number; i++) {
//			trainingDate.add(startingDate.plusMinutes(i * interval));
//			trainingData.add(i + 0.00);
//			j++;
//
//		}
//
//		// generating validation data
//
//		ArrayList<Double> validationData = new ArrayList<Double>();
//		ArrayList<OffsetDateTime> validationDate = new ArrayList<OffsetDateTime>();
//
//		// generating 10 data points
//		OffsetDateTime startingvalDate = OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(1));
//		for (int i = 0; i < number; i++) {
//			validationDate.add(startingvalDate.plusMinutes(i * interval));
//			validationData.add(i + j + 0.00);
//
//		}
//
//		new TrainAndValidateBatch(trainingData, trainingDate, validationData, validationDate,hyperParameters);
//	}
//	
//	
//
//}
