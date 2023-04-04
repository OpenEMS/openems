package io.openems.edge.predictor.lstmmodel.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Preprocessing {

	public static double trainSplit = 0.6;
	public static double validateSplit = 0.2;

	int LenTrain = 0;
	int LenValidate = 0;

	ArrayList<ArrayList<Double>> trainDataList;
	ArrayList<ArrayList<Double>> validateDataList;
	ArrayList<ArrayList<Double>> testDataList;

	ArrayList<Double> trainTargetList;
	ArrayList<Double> validataTargetList;
	ArrayList<Double> testTargetList;

	ArrayList<Double> dataList;
	ArrayList<Double> scaleDataList;

	public double[][] trainData;
	public double[][] validateData;
	public double[][] testData;

	public double[] trainTarget;
	public double[] validateTarget;
	public double[] testTarget;

	/**
	 * Constructor with out explicit train/ validate split percentage
	 * 
	 * @param data       {@link List<Double>} data
	 * @param windowSize size of the window
	 */
	public Preprocessing(List<Double> data, int windowSize) {
		this(data, windowSize, trainSplit, validateSplit);
	}

	/**
	 * Constructor with train/ validate split percentage
	 * 
	 * @param data          {@link List<Double>} data
	 * @param windowSize    size of the window
	 * @param trainSplit    percent of the trainSplit (0.6 for 60 percent)
	 * @param validateSplit percent of the validateSplit (0.2 for 20 percent)
	 */
	public Preprocessing(List<Double> data, int windowSize, double trainSplit, double validateSplit) {
		System.out.println("Starting Preprocessing ....");
		this.dataList = (ArrayList<Double>) data;
		this.scaleDataList = new ArrayList<Double>();
		this.trainDataList = new ArrayList<ArrayList<Double>>();
		this.validateDataList = new ArrayList<ArrayList<Double>>();
		this.testDataList = new ArrayList<ArrayList<Double>>();
		this.trainTargetList = new ArrayList<Double>();
		this.validataTargetList = new ArrayList<Double>();
		this.testTargetList = new ArrayList<Double>();

		this.LenTrain = (int) (trainSplit * this.dataList.size());
		this.LenValidate = this.LenTrain + (int) (validateSplit * this.dataList.size());
		System.out.println(" Total Data size : " + this.dataList.size());
		System.out.println(" Train Data from : " + 0 + " to " + this.LenTrain + " index");
		System.out.println(" Validate Data from : " + this.LenTrain + " to " + this.LenValidate + " index");
		System.out.println(" Test Data from : " + this.LenValidate + " to " + this.dataList.size() + " index");

		double minScaled = 0.2;
		double maxScaled = 0.8;
		System.out.println("Scaling the data with minScaled :" + minScaled + " and maxScaled :" + maxScaled + "....");
		scale(minScaled, maxScaled);

		//System.out.println(this.scaleDataList);

		TrainTestValidateData(0, this.LenTrain, windowSize, ConverDataType.TRAIN);
		TrainTestValidateTarget(0, this.LenTrain, windowSize, ConverDataType.TRAIN);
		TrainTestValidateData(this.LenTrain, this.LenValidate, windowSize, ConverDataType.VALIDATE);
		TrainTestValidateTarget(this.LenTrain, this.LenValidate, windowSize, ConverDataType.VALIDATE);
		TrainTestValidateData(this.LenValidate, this.dataList.size(), windowSize, ConverDataType.TEST);
		TrainTestValidateTarget(this.LenValidate, this.dataList.size(), windowSize, ConverDataType.TEST);
		convertData(ConverDataType.TRAIN);
		convertData(ConverDataType.VALIDATE);
		convertData(ConverDataType.TEST);
		convertTarget(ConverDataType.TRAIN);
		convertTarget(ConverDataType.VALIDATE);
		convertTarget(ConverDataType.TEST);

	}

	public void scale(double minScaled, double maxScaled) {
		double max = Collections.max(this.dataList);
		double min = Collections.min(this.dataList);
		this.scaleDataList = (ArrayList<Double>) this.dataList.stream() //
				.map(item -> (((item - min) / max) * (maxScaled - minScaled)) + minScaled) //
				.collect(Collectors.toList());
	}

	public void TrainTestValidateTarget(int lower, int upper, int window, ConverDataType converDataType) {
		for (int i = lower; i < (upper - window); i++) {
			switch (converDataType) {
			case VALIDATE:
				validataTargetList.add((double) this.scaleDataList.get(i + window));
				break;
			case TEST:
				testTargetList.add((double) this.scaleDataList.get(i + window));
				break;
			case TRAIN:
				trainTargetList.add(this.scaleDataList.get(i + window));
				break;
			default:
				System.out.println(
						"If you are seeing this, there is an error in TrainTestValidateTarget method of Preprocessing Class");
				break;
			}
		}
	}

	/**
	 * 
	 * @param lower
	 * @param upper
	 * @param window
	 * @param converDataType
	 */
	public void TrainTestValidateData(int lower, int upper, int window, ConverDataType converDataType) {

		for (int i = lower; i < upper - window; i++) {
			ArrayList<Double> temp = new ArrayList<Double>();
			for (int j = 0; j < window; j++) {
				double b = this.scaleDataList.get(i + j);
				temp.add(b);

			}
			switch (converDataType) {
			case TEST:
				testDataList.add(temp);
				break;
			case TRAIN:
				trainDataList.add(temp);
				break;
			case VALIDATE:
				validateDataList.add(temp);
				break;
			default:
				System.out.println(
						"If you are seeing this, there is an error in TrainTestValidate method of Preprocessing Class");
				break;

			}
		}
	}
	
	

	/**
	 * Converts ArrayList<ArrayList<Double> to double[][]
	 * 
	 * @param convertDataType {@link ConverDataType}
	 */
	public void convertData(ConverDataType convertDataType) {

		switch (convertDataType) {
		case TRAIN:
			this.trainData = convert2DArrayListTo2DArray(this.trainDataList);
			break;
		case VALIDATE:
			this.validateData = convert2DArrayListTo2DArray(this.validateDataList);
			break;
		case TEST:
			this.testData = convert2DArrayListTo2DArray(this.testDataList);
			break;
		default:
			System.out.println("Something is wrong in preprocessing, check convertdata()");
			break;
		}
	}

	public double[][] convert2DArrayListTo2DArray(ArrayList<ArrayList<Double>> data) {
		return data.stream() //
				.map(l -> l.stream() //
						.mapToDouble(Double::doubleValue) //
						.toArray()) //
				.toArray(double[][]::new);
	}

	public void convertTarget(ConverDataType convertDataType) {
		switch (convertDataType) {
		case TRAIN:
			this.trainTarget = convert1DArrayListTo1DArray(this.trainTargetList);
			break;
		case TEST:
			this.testTarget = convert1DArrayListTo1DArray(this.testTargetList);
			break;
		case VALIDATE:
			this.validateTarget = convert1DArrayListTo1DArray(this.validataTargetList);
			break;
		default:
			System.out.println("Something is wrong in preprocessing, convertTarget()");
			break;
		}

	}

	public double[] convert1DArrayListTo1DArray(ArrayList<Double> data) {
		return data.stream().mapToDouble(d -> d).toArray();
	}

	/**
	 * Simple enum for conversion types
	 * 
	 * <ul>
	 * -1 is to convert train data
	 * </ul>
	 * 
	 * <ul>
	 * 0 is to convert validation data
	 * </ul>
	 * 
	 * <ul>
	 * 1 is to convert test data
	 * </ul>
	 */
	public enum ConverDataType {
		TRAIN(-1), //
		VALIDATE(0), //
		TEST(1);

		private int numVal;

		ConverDataType(int numVal) {
			this.numVal = numVal;
		}

		public int getNumVal() {
			return numVal;
		}
	}

}
