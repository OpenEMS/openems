package io.openems.edge.predictor.lstmmodel;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;

import io.openems.edge.predictor.lstmmodel.preprocessing.ReadCsv;
import io.openems.edge.predictor.lstmmodel.util.MakeModel;

public class StandAloneMakeModelTest {

	@Test
	public void test() {
		ReadCsv csv = new ReadCsv();
		ArrayList<Double> values = csv.data;
		ArrayList<OffsetDateTime> dates = csv.dates;
		double minOfTrainingData=Collections.max(values);
		double maxOfTrainingData =Collections.min(values);
		MakeModel obj = new MakeModel(values,dates,minOfTrainingData,maxOfTrainingData);
		
	}

}
