package io.openems.edge.predictor.lstmmodel.util;
import io.openems.edge.predictor.lstmmodel.validation.Validation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;

import io.openems.edge.predictor.lstmmodel.interpolation.InterpolationManager;
import io.openems.edge.predictor.lstmmodel.model.SaveModel;
import io.openems.edge.predictor.lstmmodel.preprocessing.PreProcessingImpl;
import io.openems.edge.predictor.lstmmodel.preprocessing.ReadCsv;
import io.openems.edge.predictor.lstmmodel.preprocessing.GroupBy;
import io.openems.edge.predictor.lstmmodel.preprocessing.Suffle;
import io.openems.edge.predictor.lstmmodel.util.Engine.EngineBuilder;
import io.openems.edge.predictor.lstmmodel.predictor.ReadModels;
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
	ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> weightMatrix = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
	ArrayList<ArrayList<Double>> weight1 = new ArrayList<ArrayList<Double>>();
	ArrayList<ArrayList<Double>> weight2 = new ArrayList<ArrayList<Double>>();
   
	/**
	 * ArrayList to store grouped data and dates according to minutes of hour e.g.
	 * the data of 2:15 pm of entire year are stored in one arraylist
	 * 
	 */
	ArrayList<ArrayList<Double>> modle = new ArrayList<ArrayList<Double>>();

	public makeMultipleModel() {
		// weight1 =  generateInitialWeightMatrix(7);
		ReadModels mo =  new ReadModels();

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
			System.out.println("");
			System.out.println(i+1+"/"+dataGroupedByMinute.size());
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
							.setValidatorCounter(2500)//
							.build();
				

					int epochs = 5000;
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
					//System.out.println(mo.allModel.get(mo.allModel.size()-1).get(j));
					
					
					model.fit(epochs,mo.allModel.get(mo.allModel.size()-1).get(j));
					
					weight1 = model.weights.get(model.weights.size()-1);
					//weight2=weight1;
					weightMatrix.add(model.weights);
					/*
					 * checking condition for validation
					 * This can be modified
					 */
//					if (weightMatrix.size()>=75) {
//						SaveModel save =new SaveModel();
//						save.saveModels(weightMatrix);
//
//					
//						Validation obj3 = new Validation();
//						ReadModels obj4 = new ReadModels();
//						weightMatrix = obj4.allModel;
//						
//					}
					
				}

				catch (Exception e) {
					e.printStackTrace();

				}
				
				
			}

			/**
			 * saving Model as .txt file
			 */

		}
		SaveModel save1 =new SaveModel();
		save1.saveModels(weightMatrix);
		
		//Validation obj3 = new Validation();
		//ReadModels obj4 = new ReadModels();
		//weightMatrix = obj4.allModel;
		
//		SaveModel save =new SaveModel();
//		save.saveModels(weightMatrix);

		//saveModel(weightMatrix);
		System.out.println("Model Saved");
	}

//	public void saveModel(ArrayList<ArrayList<ArrayList<Double>>> weightMatrix) {
//		// String path="C:\\Users\\bishal.ghimire\\Desktop\\data\\model.txt";
//		try {
//			String filename = "\\testResults\\model.txt";
//			String path = new File(".").getCanonicalPath() + filename;
//			FileWriter fw = new FileWriter(path);
//			BufferedWriter bw = new BufferedWriter(fw);
//
//			for (ArrayList<ArrayList<Double>> innerList : weightMatrix) {
//				for (ArrayList<Double> innerInnerList : innerList) {
//					for (Double value : innerInnerList) {
//						bw.write(value.toString() + " ");
//					}
//					bw.newLine();
//				}
//				bw.newLine();
//			}
//
//			bw.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

	
public ArrayList<ArrayList<Double>> generateInitialWeightMatrix(int windowSize){
	ArrayList<ArrayList<Double>> initialWeight = new ArrayList<ArrayList<Double>>();
	ArrayList<Double> temp1 = new ArrayList<Double>();
	ArrayList<Double> temp2 = new ArrayList<Double>();
	ArrayList<Double> temp3 = new ArrayList<Double>();
	ArrayList<Double> temp4 = new ArrayList<Double>();
	ArrayList<Double> temp5 = new ArrayList<Double>();
	ArrayList<Double> temp6 = new ArrayList<Double>();
	ArrayList<Double> temp7 = new ArrayList<Double>();
	ArrayList<Double> temp8 = new ArrayList<Double>();
	
	for(int i =1; i<=windowSize;i++)
	
	{
		double wi=1.00;
		double wo=1.00;
		double wz=1.00;
		double ri=1.00;
		double ro=1.00;
		double rz=1.00;
		double ct=1.00;
		double yt=1.00;
		
		temp1.add(wi);
		temp2.add(wo);
		temp3.add(wz);
		temp4.add(ri);
		temp5.add(ro);
		temp6.add(rz);
		temp7.add(yt);
		temp8.add(ct);
		
		
		
	}
	initialWeight.add(temp1);
	initialWeight.add(temp2);
	initialWeight.add(temp3);
	initialWeight.add(temp4);
	initialWeight.add(temp5);
	initialWeight.add(temp6);
	initialWeight.add(temp7);
	initialWeight.add(temp8);
	
	
	return initialWeight;
	
	
}

}
