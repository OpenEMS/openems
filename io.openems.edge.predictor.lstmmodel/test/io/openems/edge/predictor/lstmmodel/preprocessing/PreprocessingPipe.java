//package io.openems.edge.predictor.lstm.preprocessing;
//
//import static org.junit.Assert.assertEquals;
//
//import java.util.ArrayList;
//
//import org.junit.Test;
//
//import io.openems.edge.predictor.lstm.common.HyperParameters;
//import io.openems.edge.predictor.lstm.preprocessingpipeline.PreprocessingPipeImpl;
//import io.openems.edge.predictor.lstm.preprocessingpipeline.ScalingPipe;
//import io.openems.edge.predictor.lstm.preprocessingpipeline.TrainandTestSplitPipe;
//import io.openems.edge.predictor.lstm.utilities.UtilityConversion;
//
//public class PreprocessingPipe {
//
//	@Test
//	public void trainandTestSplitPipeTest() {
//		double[] data = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 };
//		double[] res1 = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, };
//		double[] res2 = { 7.0, 8.0, 9.0, 10.0 };
//		HyperParameters hyp = new HyperParameters();
//		hyp.setDatasplitTrain(.7);
//		TrainandTestSplitPipe ttsp = new TrainandTestSplitPipe(hyp);
//		assertEquals(ttsp.execute(data)[0], res1);
//		assertEquals(ttsp.execute(data)[1], res2);
//
//	}
//
//	@Test
//
//	public void scalingPipeTest() {
//		double[] data = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 };
//
//		double[] result = { 0.2, 0.2666666666666667, 0.33333333333333337, 0.4, 0.46666666666666673, 0.5333333333333334,
//				0.6000000000000001, 0.6666666666666667, 0.7333333333333334, 0.8 };
//
//		HyperParameters hyp = new HyperParameters();
//		hyp.setScalingMax(10.0);
//		hyp.setScalingMin(1);
//		ScalingPipe sp = new ScalingPipe(hyp);
//
//	}
//
//	@Test
//	public void preprocessingPiplineTest() {
//		double[] data = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0,
//				18.0 };
//		HyperParameters hyp = new HyperParameters();
//		hyp.setScalingMax(100);
//		hyp.setScalingMin(1);
//		hyp.setDatasplitTrain(.7);
//		hyp.setWindowSizeTrend(2);
//		hyp.setWindowSizeSeasonality(7);
//
//		PreprocessingPipeImpl ppimplObj1 = new PreprocessingPipeImpl(hyp);
//		ArrayList<ArrayList<Double>> temp = UtilityConversion
//				.convert2DArrayTo2DArrayList((double[][]) ppimplObj1.setData(data).scale().trainTestSplit().execute());
//		// System.out.println(temp);
//
//		PreprocessingPipeImpl ppimplObj2 = new PreprocessingPipeImpl(hyp);
//		double[][][] normalized = (double[][][]) ppimplObj2.setData(data).groupToStiffedWindow().execute();
//		ArrayList<ArrayList<Double>> normalizedData = UtilityConversion.convert2DArrayTo2DArrayList(normalized[0]);
//		ArrayList<Double> target = UtilityConversion.convert1DArrayTo1DArrayList(normalized[1][0]);
//
//		System.out.println("Target = " + target);
//		System.out.println("data = " + normalizedData);
//
//	}
//
//}
