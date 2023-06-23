package io.openems.edge.predictor.lstmmodel.predictor;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import io.openems.edge.predictor.lstmmodel.preprocessing.GroupBy;
import io.openems.edge.predictor.lstmmodel.preprocessing.PreProcessing;
import io.openems.edge.predictor.lstmmodel.preprocessing.RMSErrorCalculator;
//import com.example.LstmPredictorImpl.Predictor;
import io.openems.edge.predictor.lstmmodel.interpolation.InterpolationManager;
import io.openems.edge.predictor.lstmmodel.predictor.ScaleBack;
import io.openems.edge.predictor.lstmmodel.predictor.Preprocessing;

public class Prediction {
	public ArrayList<Double> predictedAndScaledBack = new ArrayList<Double>();
	public ArrayList<Double> dataShouldBe =  new ArrayList<Double>();

	

	public Prediction(double minOfTrainingData,double maxOfTrainingData) {
		// TODO Auto-generated method stub
		// on calling this we should get 96 data points

		ArrayList<ArrayList<ArrayList<OffsetDateTime>>> dateGroupedByMinute = new ArrayList<ArrayList<ArrayList<OffsetDateTime>>>();
		ArrayList<ArrayList<Double>> dataGroupedByMinute1 = new ArrayList<ArrayList<Double>>();

		ArrayList<ArrayList<ArrayList<Double>>> dataGroupedByMinute = new ArrayList<ArrayList<ArrayList<Double>>>();
		ArrayList<Double> scaledData = new ArrayList<Double>();
		ArrayList<Double> predicted = new ArrayList<Double>();
		//ArrayList<Double> predictedAndScaledBack = new ArrayList<Double>();

		Data obj = new Data(10);
		

		ArrayList<Double> dataToPredict = obj.sevenDaysData;
		ArrayList<OffsetDateTime> dateToPredict = obj.sevenDayDates;
		//int windowSize = 7;

		//System.out.println(dataToPredict.size());

		dataShouldBe = obj.eighthDayData;
		ArrayList<OffsetDateTime> dateShouldBe = obj.eighthDayDate;
//		System.out.println(dateShouldBe);
//		System.out.println(dataShouldBe);

		/**
		 * Interpolate
		 */

		InterpolationManager obj1 = new InterpolationManager(dataToPredict);
		dataToPredict = obj1.interpolated;

		/**
		 * Scaling
		 */

		Preprocessing obj4 = new Preprocessing(dataToPredict);
		obj4.scale();
		scaledData = obj4.scaledData;
		/**
		 * Grouping data by hour
		 */
		GroupBy obj2 = new GroupBy(scaledData, dateToPredict);

		obj2.hour();

		/**
		 * Grouping data by minute
		 */

		for (int i = 0; i < obj2.groupedDataByHour.size(); i++) {

			GroupBy obj3 = new GroupBy(obj2.groupedDataByHour.get(i), obj2.groupedDateByHour.get(i));
			obj3.minute();
			dataGroupedByMinute.add(obj3.groupedDataByMin);
			dateGroupedByMinute.add(obj3.groupedDateByMin);
		}
////		System.out.println(dataGroupedByMinute);
////		System.out.println(dateGroupedByMinute);

		

		for (int i = 0; i < dataGroupedByMinute.size(); i++) {
			for (int j = 0; j < dataGroupedByMinute.get(i).size(); j++) {
////			 Preprocessing obj4 =new Preprocessing(dataGroupedByMinute.get(i).get(j));
////			 obj4.scale();
////			 scaledData.add(obj4.scaledData);
				dataGroupedByMinute1.add(dataGroupedByMinute.get(i).get(j));
////			 
			}

		}

	/**
		 * Make prediction
		 */
		predicted = Predictor.Predict(dataGroupedByMinute1, obj.model.dataList);
		

		/**
		 * scale back //
		 */
////		int k=0;
		for (int i = 0; i<predicted.size();i++) {
////			 for (int j = 0; j<dataGroupedByMinute.get(i).size();j++) {
			
////				 
				 predictedAndScaledBack.add(ScaleBack.scaleBack(dataToPredict,predicted.get(i),minOfTrainingData,maxOfTrainingData));
////				 k=k+1;
////				 
////		
		}
////		}
////	
double error= RMSErrorCalculator.calculateRMSError( dataShouldBe, predictedAndScaledBack);
//		
///for (int i = 0; i<predictedAndScaledBack.size();i++) {		
//System.out.println( predictedAndScaledBack);	
////System.out.print("    ,      ");
//System.out.println(dataShouldBe);
//System.out.print(dataGroupedByMinute);

//
////System.out.println("");
////		
////		
////	}
//System.out.println("Predicted: "+predictedAndScaledBack);
//System.out.println("Orginal: "+dataShouldBe);
//
//System.out.println(error);

//System.out.println( predictedAndScaledBack);	

	}
}
