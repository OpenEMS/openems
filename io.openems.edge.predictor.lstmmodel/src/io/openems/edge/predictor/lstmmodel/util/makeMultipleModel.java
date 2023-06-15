package io.openems.edge.predictor.lstmmodel.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;

import io.openems.edge.predictor.lstmmodel.interpolation.InterpolationManager;
import io.openems.edge.predictor.lstmmodel.preprocessing.PreProcessingImpl;
import io.openems.edge.predictor.lstmmodel.preprocessing.ReadCsv;
import io.openems.edge.predictor.lstmmodel.preprocessing.GroupBy;
import io.openems.edge.predictor.lstmmodel.preprocessing.ReadCsv;
import io.openems.edge.predictor.lstmmodel.preprocessing.Suffle;
import io.openems.edge.predictor.lstmmodel.util.Engine.EngineBuilder;

/**
 * 
 * @author bishal.ghimire class to make 96 model
 *
 */
public class makeMultipleModel {

	ArrayList<Double> values;
	ArrayList<OffsetDateTime> dates;
	ArrayList<ArrayList<ArrayList<OffsetDateTime>>> dateGroupedByMinute = new ArrayList<ArrayList<ArrayList<OffsetDateTime>>>();
	ArrayList<ArrayList<ArrayList<Double>>> dataGroupedByMinute = new ArrayList<ArrayList<ArrayList<Double>>>();
	ArrayList<ArrayList<ArrayList<Double>>> weightMatrix = new ArrayList<ArrayList<ArrayList<Double>>>();
	ArrayList<ArrayList<Double>> weight1 = new ArrayList<ArrayList<Double>>();

	/**
	 * ArrayList to store grouped data and dates according to minutes of hour e.g.
	 * the data of 2:15 pm of entire year are stored in one arraylist
	 * 
	 */
	ArrayList<ArrayList<Double>> modle = new ArrayList<ArrayList<Double>>();

	public makeMultipleModel() {

		ReadCsv csv = new ReadCsv();
		values = csv.data;
		dates = csv.dates;
		/**
		 * compute interpolation
		 */
		InterpolationManager inter = new InterpolationManager(values); // The result of interpolation is in
																		// inter.interpolated
		/**
		 * Grouping the interpolated data by hour
		 */

		GroupBy groupAsHour = new GroupBy(inter.interpolated, dates);// The result are stored in
																		// groupAS.groupedDateByHour and
																		// groupAs.groupedDataByHour
		groupAsHour.hour();

		/**
		 * Grouping data by minute
		 */

		for (int i = 0; i < groupAsHour.groupedDataByHour.size(); i++) {
			GroupBy groupAsMinute = new GroupBy(groupAsHour.groupedDataByHour.get(i),
					groupAsHour.groupedDateByHour.get(i));
			groupAsMinute.minute();
			dataGroupedByMinute.add(groupAsMinute.groupedDataByMin);
			dateGroupedByMinute.add(groupAsMinute.groupedDateByMin);
		}
		/**
		 * compute model
		 */
		ArrayList<ArrayList<ArrayList<Double>>> allModel = new ArrayList<ArrayList<ArrayList<Double>>>();
		for (int i = 0; i < dataGroupedByMinute.size(); i++) {
			for (int j = 0; j < dataGroupedByMinute.get(i).size(); j++) {
				int windowsSize = 7;
				PreProcessingImpl preprocessing = new PreProcessingImpl(dataGroupedByMinute.get(i).get(j), windowsSize);
				preprocessing.scale(0.2, 0.8);
				try {

					double[][] trainData = preprocessing.getFeatureData(preprocessing.trainTestSplit.trainIndexLower,
							preprocessing.trainTestSplit.trainIndexHigher);

					double[][] validateData = preprocessing.getFeatureData(
							preprocessing.trainTestSplit.validateIndexLower,
							preprocessing.trainTestSplit.validateIndexHigher);
					
					double[][] testData = preprocessing.getFeatureData(preprocessing.trainTestSplit.testIndexLower,
							preprocessing.trainTestSplit.testIndexHigher);

					double[] trainTarget = preprocessing.getTargetData(preprocessing.trainTestSplit.trainIndexLower,
							preprocessing.trainTestSplit.trainIndexHigher);

					double[] validateTarget = preprocessing.getTargetData(
							preprocessing.trainTestSplit.validateIndexLower,
							preprocessing.trainTestSplit.validateIndexHigher);
					
					Suffle obj1 = new Suffle(trainData,trainTarget);
					Suffle obj2 = new Suffle(validateData, validateTarget);
					
					EngineBuilder modelTemp = new EngineBuilder();

					Engine model = new EngineBuilder() //
							.setInputMatrix(obj1.data) //
							.setTargetVector(obj1.target) //
							.setValidateData(obj2.data) //
							.setValidateTarget(obj2.target) //
							.setValidatorCounter(60)//
							.build();
				

					int epochs = 1000;
//					if (i==0 && j==0) {
//					
//						
//					}
//					
//					else {
//						//System.exit(0);
//						for(int k=0;k<model.generalLstm.cells.size();k++) {
//							model.generalLstm.initilizeCells();
//							model.generalLstm.cells.get(k).setWi(weight1.get(0).get(k));
//							model.generalLstm.cells.get(k).setWo(weight1.get(1).get(k));
//							model.generalLstm.cells.get(k).setWz(weight1.get(2).get(k));
//							model.generalLstm.cells.get(k).setRi(weight1.get(3).get(k));
//							model.generalLstm.cells.get(k).setRo(weight1.get(4).get(k));
//							model.generalLstm.cells.get(k).setRz(weight1.get(5).get(k));
//							
//						
//					}
//					
//					
//
//				}
					model.fit(epochs);
					weight1 = model.finalWeight;
					weightMatrix.add(model.finalWeight);
				}

				catch (Exception e) {
					e.printStackTrace();

				}
			}

			/**
			 * saving Model as .txt file
			 */

		}

		saveModel(weightMatrix);
		System.out.println("Model Saved");
	}

	public void saveModel(ArrayList<ArrayList<ArrayList<Double>>> weightMatrix) {
		// String path="C:\\Users\\bishal.ghimire\\Desktop\\data\\model.txt";
		try {
			String filename = "\\testResults\\model.txt";
			String path = new File(".").getCanonicalPath() + filename;
			FileWriter fw = new FileWriter(path);
			BufferedWriter bw = new BufferedWriter(fw);

			for (ArrayList<ArrayList<Double>> innerList : weightMatrix) {
				for (ArrayList<Double> innerInnerList : innerList) {
					for (Double value : innerInnerList) {
						bw.write(value.toString() + " ");
					}
					bw.newLine();
				}
				bw.newLine();
			}

			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
