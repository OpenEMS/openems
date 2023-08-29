//package io.openems.edge.predictor.lstm.preprocessing;
//package io.openems.edge.predictor.lstmmodel.preprocessing;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.stream.Collectors;
//import java.util.stream.DoubleStream;
//import java.util.stream.Stream;
//
//import io.openems.edge.predictor.lstmmodel.util.Cell.propagationType;
//import io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion;
//
//@Deprecated
//public class PreprocessingImpl2 implements PreProcessing {
//
//	public static double trainSplit = 0.6;
//	public static double validateSplit = 0.2;
//
//	double max = 0;
//	double min = 0;
//	int LenTrain = 0;
//	int LenValidate = 0;
//
//	ArrayList<ArrayList<Double>> trainDataList;
//	ArrayList<ArrayList<Double>> validateDataList;
//	ArrayList<ArrayList<Double>> testDataList;
//
//	ArrayList<Double> trainTargetList;
//	ArrayList<Double> validataTargetList;
//	ArrayList<Double> testTargetList;
//
//	ArrayList<Double> dataList;
//	ArrayList<Double> scaleDataList;
//	ArrayList<Double> reverseScaleDataList;
//
//	public double[][] trainData;
//	public double[][] validateData;
//	public double[][] testData;
//
//	public double[] trainTarget;
//	public double[] validateTarget;
//	public double[] testTarget;
//
//	/**
//	 * Constructor with out explicit train/ validate split percentage
//	 * 
//	 * @param data       {@link List<Double>} data
//	 * @param windowSize size of the window
//	 */
//	public PreprocessingImpl2(List<Double> data, int windowSize) {
//		this(data, windowSize, trainSplit, validateSplit);
//	}
//	
//	
//
//	/**
//	 * Constructor with train/ validate split percentage
//	 * 
//	 * @param data          {@link List<Double>} data
//	 * @param windowSize    size of the window
//	 * @param trainSplit    percent of the trainSplit (0.6 for 60 percent)
//	 * @param validateSplit percent of the validateSplit (0.2 for 20 percent)
//	 */
//	public PreprocessingImpl2(List<Double> data, int windowSize, double trainSplit, double validateSplit) {
//		System.out.println("Starting Preprocessing ....");
//		this.dataList = (ArrayList<Double>) data;
//
//		max = Collections.max(this.dataList);
//		min = Collections.min(this.dataList);
//
//
//
//		this.LenTrain = (int) (trainSplit * this.dataList.size());
//		this.LenValidate = this.LenTrain + (int) (validateSplit * this.dataList.size());
//
//		int totalSize = this.dataList.size() - 1;
//		int TrainDataSize = 2000;
//		int validDataSize = 2278;
//		int testDataSize = 2279;
//
//		System.out.println(" Total Data size : " + totalSize);
//		System.out.println(" Train Data from : " + 0 + " to " + TrainDataSize);
//		System.out.println(" Validate Data from : " + (TrainDataSize + 1) + " to " + validDataSize + " index");
//		System.out.println(" Test Data from : " + testDataSize + " to " + totalSize + " index");
//
//		double minScaled = 0.2;
//		double maxScaled = 0.8;
//		System.out.println("Scaling the data with minScaled :" + minScaled + " and maxScaled :" + maxScaled + "....");
//		scaleDataList = scale(minScaled, maxScaled);
//
//		// System.out.println(this.scaleDataList);
//
////		TrainTestValidateData(0, this.LenTrain, windowSize, ConverDataType.TRAIN);
////		TrainTestValidateTarget(0, this.LenTrain, windowSize, ConverDataType.TRAIN);
////		TrainTestValidateData(this.LenTrain, this.dataList.size() - 93, windowSize, ConverDataType.VALIDATE);
////		TrainTestValidateTarget(this.LenTrain, this.dataList.size() - 93, windowSize, ConverDataType.VALIDATE);
////		TrainTestValidateData(this.dataList.size() - 92, this.dataList.size(), windowSize, ConverDataType.TEST);
////		TrainTestValidateTarget(this.dataList.size() - 92, this.dataList.size(), windowSize, ConverDataType.TEST);
//
//		try {
//			validateDataList = getFeatureData((TrainDataSize + 1), validDataSize, windowSize, ConverDataType.VALIDATE);
//			trainDataList = getFeatureData(0, TrainDataSize, windowSize, ConverDataType.TRAIN);
//			testDataList = getFeatureData(testDataSize, totalSize, windowSize, ConverDataType.TEST);
//			trainTargetList = getTargetData(0, TrainDataSize, windowSize, ConverDataType.TRAIN);
//			validataTargetList = getTargetData((TrainDataSize + 1), validDataSize, windowSize, ConverDataType.VALIDATE);
//			testTargetList = getTargetData(testDataSize, totalSize, windowSize, ConverDataType.TEST);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		trainData = UtilityConversion.convert2DArrayListTo2DArray(trainDataList);// convertData(ConverDataType.TRAIN);
//		validateData = UtilityConversion.convert2DArrayListTo2DArray(validateDataList);// convertData(ConverDataType.VALIDATE);
//		testData = UtilityConversion.convert2DArrayListTo2DArray(testDataList);// convertData(ConverDataType.TEST);
//
//		trainTarget = UtilityConversion.convert1DArrayListTo1DArray(trainTargetList);// convertTarget(ConverDataType.TRAIN);
//		validateTarget = UtilityConversion.convert1DArrayListTo1DArray(validataTargetList);// convertTarget(ConverDataType.VALIDATE);
//		testTarget = UtilityConversion.convert1DArrayListTo1DArray(testTargetList);// convertTarget(ConverDataType.TEST);
//
//	}
//
//	/**
//	 * Gets the feature data.
//	 * 
//	 * @param lower          lowest index of the datalist
//	 * @param upper          upper index of the datalist
//	 * @param window         size of the window
//	 * @param converDataType type of conversion
//	 * @return Featuredata
//	 * @throws Exception
//	 */
//	public ArrayList<ArrayList<Double>> getFeatureData(int lower, int upper, int window, ConverDataType converDataType)
//			throws Exception {
//
//		if (this.scaleDataList.isEmpty()) {
//			throw new Exception("Scaled data is empty");
//		}
//
//		ArrayList<ArrayList<Double>> featureDataList = new ArrayList<ArrayList<Double>>();
//		for (int i = lower; i < upper - window; i++) {
//			ArrayList<Double> temp = new ArrayList<Double>();
//			for (int j = 0; j < window; j++) {
//				double b = this.scaleDataList.get(i + j);
//				temp.add(b);
//			}
//			featureDataList.add(temp);
//		}
//		return featureDataList;
//
//	}
//
//	/**
//	 * Gets the target data.
//	 * 
//	 * @param lower          lowest index of the datalist
//	 * @param upper          upper index of the datalist
//	 * @param window         size of the window
//	 * @param converDataType type of conversion
//	 * @return targetdata
//	 * @throws Exception
//	 */
//	public ArrayList<Double> getTargetData(int lower, int upper, int window, ConverDataType converDataType)
//			throws Exception {
//
//		if (this.scaleDataList.isEmpty()) {
//			throw new Exception("Scaled data is empty");
//		}
//		ArrayList<Double> targetDataList = new ArrayList<Double>();
//
//		for (int i = lower; i < (upper - window); i++) {
//			targetDataList.add((double) this.scaleDataList.get(i + window));
//		}
//		return targetDataList;
//	}
//
//	/**
//	 * Scale the Data with min and max values of the list.
//	 * 
//	 * @param minScaled
//	 * @param maxScaled
//	 * @return scaled list
//	 */
//	public ArrayList<Double> scale(double minScaled, double maxScaled) {
//
//		return (ArrayList<Double>) this.dataList.stream() //
//				.map(item -> (((item - min) / max) * (maxScaled - minScaled)) + minScaled) //
//				.collect(Collectors.toList());
//	}
//
//}
//
