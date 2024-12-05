//package io.openems.edge.predictor.lstm.train;
//
//import java.io.IOException;
//
//import org.junit.Test;
//
//import io.openems.edge.predictor.lstm.common.DeletModels;
//import io.openems.edge.predictor.lstm.common.GetObject;
//import io.openems.edge.predictor.lstm.common.HyperParameters;
//import io.openems.edge.predictor.lstm.common.ReadCsv;
//import io.openems.edge.predictor.lstm.common.SaveObject;
//
//public class MakeModelImplementationTest {
//
//	@Test
//	public static void itter() {
//		HyperParameters hyperParameters;
//		String modelName = "Consumption";
//		try {
//			hyperParameters = (HyperParameters) GetObject.get(modelName);
//		} catch (ClassNotFoundException | IOException e) {
//			// TODO Auto-generated catch block
//			System.out.println("Creating new hyperparameter object");
//			hyperParameters = HyperParameters.getInstance();
//			hyperParameters.setModelName(modelName);
//		}
//
//		// checking if the training has been completed in previous batch
//
//		if (hyperParameters.getEpochTrack() == hyperParameters.getEpoch()) {
//			// hyperParameters.setEpochTrack(0);
//
//		}
//
//		int check = hyperParameters.getOuterLoopCount();
//		for (int i = check; i <= 8; i++) {
//			hyperParameters.setOuterLoopCount(i);
//			System.out.println("Batch:" + i + "/" + 28);
//			System.out.println("count :" + hyperParameters.getCount());
//			System.out.println("Rms Error of all train for the trend is  = " + hyperParameters.getRmsErrorTrend());
//			System.out.println(
//					"Rms Error of all train for the seasonality is  = " + hyperParameters.getRmsErrorSeasonality());
//
//			String pathTrain = Integer.toString(i + 1) + ".csv";
//			String pathValidate = Integer.toString(9) + ".csv";
//
//			ReadCsv obj1 = new ReadCsv(pathTrain);
//			final ReadCsv obj2 = new ReadCsv(pathValidate);
//
//			new TrainAndValidate(obj1.getData(), obj1.getDates(), obj2.getData(), obj2.getDates(), hyperParameters);
//
//			hyperParameters.setEpochTrack(0);
//			hyperParameters.setOuterLoopCount(hyperParameters.getOuterLoopCount() + 1);
//			SaveObject.save(hyperParameters);
//			DeletModels.delet(hyperParameters);
//		}
//
//	}
//}
