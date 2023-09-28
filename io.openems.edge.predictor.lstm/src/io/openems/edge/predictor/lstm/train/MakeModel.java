package io.openems.edge.predictor.lstm.train;

import java.time.OffsetDateTime;
import java.util.ArrayList;
//import io.openems.edge.predictor.lstmmodel.interpolation.InterpolationManager;
//import io.openems.edge.predictor.lstmmodel.model.SaveModel;
//import io.openems.edge.predictor.lstmmodel.preprocessing.PreProcessingImpl;
//import io.openems.edge.predictor.lstmmodel.preprocessing.GroupBy;
//import io.openems.edge.predictor.lstmmodel.preprocessing.Normalize;
//import io.openems.edge.predictor.lstmmodel.preprocessing.Suffle;
//import io.openems.edge.predictor.lstmmodel.util.Engine;
//import io.openems.edge.predictor.lstmmodel.util.Engine.EngineBuilder;
import java.util.Collections;

import io.openems.edge.predictor.lstm.common.DataModification;
import io.openems.edge.predictor.lstm.common.ReadModels;
import io.openems.edge.predictor.lstm.common.SaveModel;
import io.openems.edge.predictor.lstm.interpolation.InterpolationManager;
import io.openems.edge.predictor.lstm.preprocessing.GroupBy;
import io.openems.edge.predictor.lstm.preprocessing.PreProcessingImpl;
import io.openems.edge.predictor.lstm.preprocessing.Suffle;
import io.openems.edge.predictor.lstm.util.Engine;
import io.openems.edge.predictor.lstm.util.Engine.EngineBuilder;

/**
 * 
 * @author bishal.ghimire class to make 96 model
 *
 */
public class MakeModel {
	String pathSeasonality = "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\";
	String pathTrend = "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\";

	

	/**
	 * ArrayList to store grouped data and dates according to minutes of hour e.g.
	 * the data of 2:15 pm of entire year are stored in one arraylist
	 * 
	 */
	ArrayList<ArrayList<Double>> modle = new ArrayList<ArrayList<Double>>();
	public MakeModel(ArrayList<Double> data, ArrayList<OffsetDateTime> date,Integer itterNumb) {
		trainSeasonality(data,date,itterNumb);
		trinTrend(data,date, itterNumb);
		
		
	}

	public void trainSeasonality(ArrayList<Double> data, ArrayList<OffsetDateTime> date, int itterNumb) {
		ArrayList<Double> values;
		ArrayList<OffsetDateTime> dates;
		ArrayList<ArrayList<ArrayList<OffsetDateTime>>> dateGroupedByMinute = new ArrayList<ArrayList<ArrayList<OffsetDateTime>>>();
		ArrayList<ArrayList<ArrayList<Double>>> dataGroupedByMinute = new ArrayList<ArrayList<ArrayList<Double>>>();
		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> weightMatrix = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
		ArrayList<ArrayList<Double>> weight1 = new ArrayList<ArrayList<Double>>();
		double minOfTrainingData;
		double maxOfTrainingData;
		
		values = data;
		dates = date;

		// compute interpolation
		InterpolationManager inter = new InterpolationManager(values);
		minOfTrainingData = Collections.min(inter.interpolated);
		maxOfTrainingData = Collections.max(inter.interpolated);

		// Grouping the interpolated data by hour
		GroupBy groupAsHour = new GroupBy(inter.interpolated, dates);
		groupAsHour.hour();

		// Grouping data by minute

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
		
		int k=0;
		

		for (int i = 0; i < dataGroupedByMinute.size(); i++) {
			if (itterNumb ==0) {
				weight1 = generateInitialWeightMatrix(7);
				}
			
			else{ReadModels models = new ReadModels(pathSeasonality+Integer.toString(itterNumb-1)+"seasonality.txt");
			weight1 = models.allModel.get(models.allModel.size() - 1).get(k);
			}
			

			System.out.println("");
			System.out.println(i + 1 + "/" + dataGroupedByMinute.size());
			for (int j = 0; j < dataGroupedByMinute.get(i).size(); j++) {
				int windowsSize = 7;
				
				
				PreProcessingImpl preprocessing = new PreProcessingImpl(DataModification.scale(dataGroupedByMinute.get(i).get(j) , minOfTrainingData, maxOfTrainingData), windowsSize);
//				preprocessing.scale(0.2, 0.8);
				try {

					double[][] trainData = preprocessing.getFeatureData(preprocessing.trainTestSplit.trainIndexLower,
							preprocessing.trainTestSplit.trainIndexHigher);

					double[][] validateData = preprocessing.getFeatureData(
							preprocessing.trainTestSplit.validateIndexLower,
							preprocessing.trainTestSplit.validateIndexHigher);

					double[] trainTarget = preprocessing.getTargetData(preprocessing.trainTestSplit.trainIndexLower,
							preprocessing.trainTestSplit.trainIndexHigher);

					double[] validateTarget = preprocessing.getTargetData(
							preprocessing.trainTestSplit.validateIndexLower,
							preprocessing.trainTestSplit.validateIndexHigher);

					Suffle obj1 = new Suffle(trainData, trainTarget);
					Suffle obj2 = new Suffle(validateData, validateTarget);
//					Normalize obj3 = new Normalize(obj1.data);
//					Normalize obj4 = new Normalize(obj2.data);

					Engine model = new EngineBuilder() //
							.setInputMatrix(DataModification.NormalizeData(obj1.data)) //
							.setTargetVector(obj1.target) //
							.setValidateData(DataModification.NormalizeData(obj2.data)) //
							.setValidateTarget(obj2.target) //
							.setValidatorCounter(25000*10000)//
							.build();

					int epochs = 1500;
					model.fit(epochs, weight1);
					weight1 = model.weights.get(model.weights.size() - 1);

					weightMatrix.add(model.weights);
					

				}

				catch (Exception e) {
					e.printStackTrace();

				}

			}

			/**
			 * saving Model as .txt file
			 */

		}
		SaveModel.saveModels(weightMatrix,Integer.toString(itterNumb)+"seasonality.txt");
		
		
		 

		System.out.println("Model Saved");
	
	}
	public void trinTrend(ArrayList<Double>data ,ArrayList<OffsetDateTime>date,int itterNumb)
	
	{
		
		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> weightMatrix = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
		ArrayList<ArrayList<Double>> weight1 = new ArrayList<ArrayList<Double>>();
		int windowsSize = 7;
		
		if (itterNumb ==0) {
			 weight1 = generateInitialWeightMatrix(windowsSize);
			
		}
		else {
			ReadModels mod = new ReadModels(pathTrend+Integer.toString(itterNumb-1)+"Trend.txt");
			weight1 = mod.dataList.get(0);
			
			
		}
		

		// compute interpolation
		InterpolationManager inter = new InterpolationManager(data);
		double minOfTrainingData = Collections.min(inter.interpolated);
		double maxOfTrainingData = Collections.max(inter.interpolated);
		
		
		
		//weight1 = mod.dataList.get(0);
		//System.out.println(mod);
		
		
		PreProcessingImpl preprocessing = new PreProcessingImpl(DataModification.scale(data , minOfTrainingData, maxOfTrainingData), windowsSize);
//		preprocessing.scale(0.2, 0.8);
		try {

			double[][] trainData = preprocessing.getFeatureData(preprocessing.trainTestSplit.trainIndexLower,
					preprocessing.trainTestSplit.trainIndexHigher);

			double[][] validateData = preprocessing.getFeatureData(
					preprocessing.trainTestSplit.validateIndexLower,
					preprocessing.trainTestSplit.validateIndexHigher);

			double[] trainTarget = preprocessing.getTargetData(preprocessing.trainTestSplit.trainIndexLower,
					preprocessing.trainTestSplit.trainIndexHigher);

			double[] validateTarget = preprocessing.getTargetData(
					preprocessing.trainTestSplit.validateIndexLower,
					preprocessing.trainTestSplit.validateIndexHigher);

			Suffle obj1 = new Suffle(trainData, trainTarget);
			Suffle obj2 = new Suffle(validateData, validateTarget);
//			Normalize obj3 = new Normalize(obj1.data);
//			Normalize obj4 = new Normalize(obj2.data);

			Engine model = new EngineBuilder() //
					.setInputMatrix(DataModification.NormalizeData(obj1.data)) //
					.setTargetVector(obj1.target) //
					.setValidateData(DataModification.NormalizeData(obj2.data)) //
					.setValidateTarget(obj2.target) //
					.build();

			int epochs = 1500;
			model.fit(epochs, weight1);
			weight1 = model.weights.get(model.weights.size() - 1);

			weightMatrix.add(model.weights);
			

		}

		catch (Exception e) {
			e.printStackTrace();

		}
		
		SaveModel.saveModels(weightMatrix,Integer.toString(itterNumb)+"trend.txt");
		
		
	}

	public ArrayList<ArrayList<Double>> generateInitialWeightMatrix(int windowSize) {
		ArrayList<ArrayList<Double>> initialWeight = new ArrayList<ArrayList<Double>>();
		ArrayList<Double> temp1 = new ArrayList<Double>();
		ArrayList<Double> temp2 = new ArrayList<Double>();
		ArrayList<Double> temp3 = new ArrayList<Double>();
		ArrayList<Double> temp4 = new ArrayList<Double>();
		ArrayList<Double> temp5 = new ArrayList<Double>();
		ArrayList<Double> temp6 = new ArrayList<Double>();
		ArrayList<Double> temp7 = new ArrayList<Double>();
		ArrayList<Double> temp8 = new ArrayList<Double>();

		for (int i = 1; i <= windowSize; i++)

		{
			double wi = 1;
			double wo = 1;
			double wz = 1;
			double ri = -10;
			double ro = -10;
			double rz = -10;
			double ct = 0;
			double yt = 0;

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
//public  static ArrayList<ArrayList<ArrayList<Double>>> conditionalValidation(ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> wm,double min, double max) {
//	SaveModel.saveModels(wm);
//	ReadCsv csv = new ReadCsv();
//	Validation validation = new Validation(csv.data,csv.dates,min,max);
//	ReadModels rm = new ReadModels();
//	return rm.allModel.get(rm.allModel.size()-1);
//	
//	
//	

	
	
	
//}
}
