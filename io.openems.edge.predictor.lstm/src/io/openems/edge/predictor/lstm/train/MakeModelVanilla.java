//package io.openems.edge.predictor.lstm.train;
//
//import java.time.OffsetDateTime;
//import java.util.ArrayList;
//
//import io.openems.edge.predictor.lstm.common.DataModification;
//import io.openems.edge.predictor.lstm.common.HyperParameters;
//import io.openems.edge.predictor.lstm.common.ReadModels;
//import io.openems.edge.predictor.lstm.common.SaveModel;
//import io.openems.edge.predictor.lstm.interpolation.InterpolationManager;
//import io.openems.edge.predictor.lstm.preprocessing.GroupBy;
//import io.openems.edge.predictor.lstm.preprocessing.PreProcessingImpl;
//import io.openems.edge.predictor.lstm.preprocessing.Suffle;
//import io.openems.edge.predictor.lstm.preprocessing.TrainTestSplit;
////import io.openems.edge.predictor.lstm.util.Engine;
//import io.openems.edge.predictor.lstm.util.EngineVanilla;
////import io.openems.edge.predictor.lstm.util.Engine.EngineBuilder;
//import io.openems.edge.predictor.lstm.util.EngineVanilla.EngineBuilder;
//
//public class MakeModelVanilla {
//	private String pathSeasonality = "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\";
//	private String pathTrend = "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\";
//
//	public MakeModelVanilla(ArrayList<Double> data, ArrayList<OffsetDateTime> date, HyperParameters hyperParameters) {
//		this.trainSeasonality(data, date, hyperParameters);
//		// this.trainTrend(data, date, hyperParameters);
//
//	}
//
//	/**
//	 * Trains a seasonality model using the provided data and parameters.
//	 * 
//	 * @param data            The time series data to train the model on.
//	 * @param date            The corresponding timestamps for the data.
//	 * @param hyperParameters An instance of class HyperParameters
//	 */
//
//	public void trainSeasonality(ArrayList<Double> data, ArrayList<OffsetDateTime> date,
//			HyperParameters hyperParameters) {
//		ArrayList<Double> values;
//		ArrayList<OffsetDateTime> dates;
//		ArrayList<ArrayList<ArrayList<OffsetDateTime>>> dateGroupedByMinute = new ArrayList<ArrayList<ArrayList<OffsetDateTime>>>();
//		ArrayList<ArrayList<ArrayList<Double>>> dataGroupedByMinute = new ArrayList<ArrayList<ArrayList<Double>>>();
//		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> weightMatrix = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
//		ArrayList<ArrayList<Double>> weight1 = new ArrayList<ArrayList<Double>>();
//		double minOfTrainingData;
//		final double maxOfTrainingData;
//
//		values = data;
//		dates = date;
//		int windowsSize = hyperParameters.getWindowSizeSeasonality();
//
//		// compute interpolation
//		InterpolationManager inter = new InterpolationManager(values);
//		minOfTrainingData = hyperParameters.getScalingMin();
//		maxOfTrainingData = hyperParameters.getScalingMax();
//
//		// Grouping the interpolated data by hour
//		GroupBy groupAsHour = new GroupBy(inter.getInterpolatedData(), dates);
//		groupAsHour.hour();
//
//		// Grouping data by minute
//
//		for (int i = 0; i < groupAsHour.getDataGroupedByHour().size(); i++) {
//
//			GroupBy groupAsMinute = new GroupBy(groupAsHour.getDataGroupedByHour().get(i),
//					groupAsHour.getDateGroupedByHour().get(i));
//			groupAsMinute.minute();
//			dataGroupedByMinute.add(groupAsMinute.getDataGroupedByMinute());
//			dateGroupedByMinute.add(groupAsMinute.getDateGroupedByMinute());
//		}
//		/**
//		 * compute model
//		 */
//
//		int k = 0;
//
//		for (int i = 0; i < dataGroupedByMinute.size(); i++) {
//			if (hyperParameters.getCount() == 0) {
//				weight1 = this.generateInitialWeightMatrix(windowsSize, hyperParameters);
//			} else {
//				String path = this.pathSeasonality + Integer.toString(hyperParameters.getCount() - 1)
//						+ "seasonality.txt";
//				ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> allModel = ReadModels.getModelForSeasonality(path, hyperParameters);
//
//				weight1 = allModel.get(allModel.size() - 1).get(k);
//			}
//
//			// System.out.println("");
//			// System.out.println(i + 1 + "/" + dataGroupedByMinute.size());
//			for (int j = 0; j < dataGroupedByMinute.get(i).size(); j++) {
//				System.out.println("making Model for " + Integer.toString(i) + ":" + Integer.toString(j * 15));
//
//				PreProcessingImpl preprocessing = new PreProcessingImpl(
//						DataModification.scale(dataGroupedByMinute.get(i).get(j), minOfTrainingData, maxOfTrainingData),
//						windowsSize);
//
//				try {
//					TrainTestSplit splitIndex = new TrainTestSplit(dataGroupedByMinute.get(i).get(j).size(),
//							windowsSize, hyperParameters.getDataSplitTrain(), hyperParameters.getDataSplitValidate());
//
//					double[][] trainData = preprocessing.getFeatureData(splitIndex.getTrainLowerIndex(),
//							splitIndex.getTrainUpperIndex());
//
//					double[][] validateData = preprocessing.getFeatureData(splitIndex.getValidateLowerIndex(),
//							splitIndex.getValidateUpperIndex());
//
//					double[] trainTarget = preprocessing.getTargetData(splitIndex.getTrainLowerIndex(),
//							splitIndex.getTrainUpperIndex());
//
//					double[] validateTarget = preprocessing.getTargetData(splitIndex.getValidateLowerIndex(),
//							splitIndex.getValidateUpperIndex());
//
//					Suffle obj1 = new Suffle(trainData, trainTarget);
//					Suffle obj2 = new Suffle(validateData, validateTarget);
//
//					EngineVanilla model = new EngineBuilder() //
//							.setInputMatrix(DataModification.normalizeData(obj1.getData())) //
//							.setTargetVector(obj1.getTarget()) //
//							.setValidateData(DataModification.normalizeData(obj2.getData())) //
//							.setValidateTarget(obj2.getTarget()) //
//							.setValidatorCounter(25000 * 10000)//
//							.build();
//
//					model.fit(hyperParameters.getGdIterration(), weight1, hyperParameters);
//					// weight1 = model.getWeights().get(model.getWeights().size() - 1);
//
//					weightMatrix.add(model.getWeights());
//				} catch (Exception e) {
//					e.printStackTrace();
//
//				}
//
//			}
//
//			/**
//			 * saving Model as .txt file
//			 */
//
//		}
//		SaveModel.saveModels(weightMatrix, Integer.toString(hyperParameters.getCount()) + "seasonality.txt");
//
//		System.out.println("Model for seasonality Saved");
//
//	}
//
//	/**
//	 * Trains a trend model using the provided data and parameters.
//	 *
//	 * @param data            The time series data to train the trend model on.
//	 * @param date            The corresponding timestamps for the data.
//	 * @param hyperParameters An instance of class HyperParameter
//	 */
//
//	public void trainTrend(ArrayList<Double> data, ArrayList<OffsetDateTime> date, HyperParameters hyperParameters) {
//
//		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> weightMatrix = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
//		ArrayList<ArrayList<Double>> weight1 = new ArrayList<ArrayList<Double>>();
//		int windowsSize = hyperParameters.getWindowSizeTrend();
//
//		if (hyperParameters.getCount() == 0) {
//			weight1 = this.generateInitialWeightMatrix(windowsSize, hyperParameters);
//
//		} else {
//			String path = this.pathTrend + Integer.toString(hyperParameters.getCount() - 1) + "Trend.txt";
//			ArrayList<ArrayList<ArrayList<Double>>> dataList = ReadModels.getModelForTrend(path);
//			weight1 = dataList.get(0);
//
//		}
//
//		// compute interpolation
//		InterpolationManager inter = new InterpolationManager(data);
//		double minOfTrainingData = hyperParameters.getScalingMin();
//		double maxOfTrainingData = hyperParameters.getScalingMax();
//
//		// weight1 = mod.dataList.get(0);
//		// System.out.println(mod);
//
//		PreProcessingImpl preprocessing = new PreProcessingImpl(
//				DataModification.scale(data, minOfTrainingData, maxOfTrainingData), windowsSize);
//
//		try {
//			TrainTestSplit splitIndex = new TrainTestSplit(inter.getInterpolatedData().size(), windowsSize, 0.8);
//
//			double[][] trainData = preprocessing.getFeatureData(splitIndex.getTrainLowerIndex(),
//					splitIndex.getTrainUpperIndex());
//
//			double[][] validateData = preprocessing.getFeatureData(splitIndex.getValidateLowerIndex(),
//					splitIndex.getValidateUpperIndex());
//
//			double[] trainTarget = preprocessing.getTargetData(splitIndex.getTrainLowerIndex(),
//					splitIndex.getTrainUpperIndex());
//
//			double[] validateTarget = preprocessing.getTargetData(splitIndex.getValidateLowerIndex(),
//					splitIndex.getValidateUpperIndex());
//
//			Suffle obj1 = new Suffle(trainData, trainTarget);
//			Suffle obj2 = new Suffle(validateData, validateTarget);
//
//			EngineVanilla model = new EngineBuilder() //
//					.setInputMatrix(DataModification.normalizeData(obj1.getData())) //
//					.setTargetVector(obj1.getTarget()) //
//					.setValidateData(DataModification.normalizeData(obj2.getData())) //
//					.setValidateTarget(obj2.getTarget()) //
//					.build();
//
//			int epochs = hyperParameters.getGdIterration();
//			model.fit(epochs, weight1, hyperParameters);
//			weight1 = model.getWeights().get(model.getWeights().size() - 1);
//			weightMatrix.add(model.getWeights());
//		} catch (Exception e) {
//			e.printStackTrace();
//
//		}
//
//		SaveModel.saveModels(weightMatrix, Integer.toString(hyperParameters.getCount()) + "trend.txt");
//		System.out.println("Model for trend Saved");
//
//	}
//
//	/**
//	 * Generates an initial weight matrix for a neural network with specified
//	 * parameters.
//	 *
//	 * @param windowSize      The size of the window or context for the neural
//	 *                        network.
//	 * @param hyperParameters An instance of class HyperParameter
//	 * @return An ArrayList of ArrayLists containing the initial weight matrix.
//	 */
//
//	public ArrayList<ArrayList<Double>> generateInitialWeightMatrix(int windowSize, HyperParameters hyperParameters) {
//		ArrayList<ArrayList<Double>> initialWeight = new ArrayList<ArrayList<Double>>();
//		ArrayList<Double> temp1 = new ArrayList<Double>();
//		ArrayList<Double> temp2 = new ArrayList<Double>();
//		ArrayList<Double> temp3 = new ArrayList<Double>();
//		ArrayList<Double> temp4 = new ArrayList<Double>();
//		ArrayList<Double> temp5 = new ArrayList<Double>();
//		ArrayList<Double> temp6 = new ArrayList<Double>();
//		ArrayList<Double> temp7 = new ArrayList<Double>();
//		ArrayList<Double> temp8 = new ArrayList<Double>();
//		ArrayList<Double> temp9 = new ArrayList<Double>();
//		ArrayList<Double> temp10 = new ArrayList<Double>();
//
//		for (int i = 1; i <= windowSize; i++) {
//			double wi = hyperParameters.getWiInit();
//			double wo = hyperParameters.getWoInit();
//			double wz = hyperParameters.getWzInit();
//			final double ri = hyperParameters.getRiInit();
//			final double ro = hyperParameters.getRoInit();
//			final double rz = hyperParameters.getRzInit();
//			final double ct = hyperParameters.getCtInit();
//			final double yt = hyperParameters.getYtInit();
//			final double wf = hyperParameters.getWfInit();
//			final double rf = hyperParameters.getRfInit();
//
//			temp1.add(wi);
//			temp2.add(wo);
//			temp3.add(wz);
//			temp4.add(ri);
//			temp5.add(ro);
//			temp6.add(rz);
//			temp7.add(yt);
//			temp8.add(ct);
//			temp9.add(wf);
//			temp10.add(rf);
//
//		}
//		initialWeight.add(temp1);
//		initialWeight.add(temp2);
//		initialWeight.add(temp3);
//		initialWeight.add(temp4);
//		initialWeight.add(temp5);
//		initialWeight.add(temp6);
//		initialWeight.add(temp7);
//		initialWeight.add(temp8);
//		initialWeight.add(temp9);
//		initialWeight.add(temp10);
//
//		return initialWeight;
//
//	}
//
//}
