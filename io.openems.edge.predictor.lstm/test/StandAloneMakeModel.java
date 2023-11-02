package io.openems.edge.predictor.lstm;

import static org.junit.Assert.*;

import org.junit.Test;

import io.openems.edge.predictor.lstm.common.ReadCsv;
import io.openems.edge.predictor.lstm.train.MakeModel;
import io.openems.edge.predictor.lstm.validator.Validation;

public class StandALoneMakeModel  {
	

	int itterNumb =27;
	String pathTrain = Integer.toString(itterNumb+1)+".csv";
	String pathValidate=Integer.toString(itterNumb+2)+".csv";
	ReadCsv obj1 = new ReadCsv(pathTrain);
	ReadCsv obj2 = new ReadCsv(pathValidate);
	//MakeModel obj = new MakeModel(obj1.data,obj1.dates,itterNumb);
	Validation obj3= new Validation(obj2.data, obj2.dates,itterNumb);
	
	@Test
	public void test() {
		
	}
}

