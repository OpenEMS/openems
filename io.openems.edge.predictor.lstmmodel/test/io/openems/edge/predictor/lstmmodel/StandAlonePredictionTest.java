package io.openems.edge.predictor.lstmmodel;
import io.openems.edge.predictor.lstmmodel.predictor.Data;


import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;

import io.openems.edge.predictor.lstmmodel.predictor.Prediction;
import io.openems.edge.predictor.lstmmodel.preprocessing.ReadCsv;
public class StandAlonePredictionTest {

	@Test
	public void test() {
		int weekNumber = 100;
		ReadCsv csv = new ReadCsv();
		ArrayList<Double> values = csv.data;
		ArrayList<OffsetDateTime> dates = csv.dates;
		double minOfTrainingData=Collections.min(values);
		double maxOfTrainingData =Collections.max(values);
		Data dat = new Data(weekNumber);
		
		
		
Prediction obj2 = new Prediction(dat.sevenDaysData,dat.sevenDayDates,minOfTrainingData,maxOfTrainingData);

System.out.println("Predicted data Points: "+ obj2.predictedAndScaledBack);
System.out.println("Orginal data Points: "+dat.eighthDayData);
obj2.makePlot(dat.eighthDayData,obj2.predictedAndScaledBack,  weekNumber);


//		
//		System.out.println("Predicted: " + obj2.predictedAndScaledBack);
//		System.out.println("Orginal: " + obj2.dataShouldBe);
	
		
	}

}
