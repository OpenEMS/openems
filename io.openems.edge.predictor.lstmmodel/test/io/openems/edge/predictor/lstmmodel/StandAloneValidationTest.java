package io.openems.edge.predictor.lstmmodel;

import io.openems.edge.predictor.lstmmodel.preprocessing.ReadCsv;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;

import io.openems.edge.predictor.lstmmodel.validation.Validation;

public class StandAloneValidationTest {

	@Test
	public void test() {
		ReadCsv csv = new ReadCsv();
		ArrayList<Double> values = csv.data;
		ArrayList<OffsetDateTime> dates = csv.dates;
		double minOfTrainingData=Collections.max(values);
		double maxOfTrainingData =Collections.min(values);
		
		Validation val = new  Validation(values, dates, minOfTrainingData, maxOfTrainingData);
		
		//Validation obj1 = new Validation();
	}
}