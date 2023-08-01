package io.openems.edge.predictor.lstmmodel;

import org.junit.Test;

import io.openems.edge.predictor.lstmmodel.predictor.Prediction;

public class StandAlonePredictionTest {

	@Test
	public void test() {
		int weekNumber = 2;
		Prediction obj2 = new Prediction(33246,73953495,weekNumber);
		Prediction.makePlot(obj2.dataShouldBe, obj2.predictedAndScaledBack, weekNumber);
		
		System.out.println("Predicted: " + obj2.predictedAndScaledBack);
		System.out.println("Orginal: " + obj2.dataShouldBe);
	
		
	}

}
