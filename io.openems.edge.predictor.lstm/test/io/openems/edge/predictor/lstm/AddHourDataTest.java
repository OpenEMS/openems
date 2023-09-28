package io.openems.edge.predictor.lstm;
import io.openems.edge.predictor.lstm.adddata.AddHourData;
import io.openems.edge.predictor.lstm.predictor.LstmModelPredictorImpl;
import io.openems.edge.predictor.lstm.common.ReadCsv;


import static org.junit.Assert.*;
import java.time.OffsetDateTime;

import java.time.ZonedDateTime;
import java.util.ArrayList;

import org.junit.Test;

public class AddHourDataTest {

	@Test
	public void test() {
//		String pathTrain = "1.csv";
//		
//		ReadCsv obj1 = new ReadCsv(pathTrain);
//		
//		
//		OffsetDateTime target = obj1.dates.get(15);
//		System.out.println(target);
//		
//		System.out.println(AddHourData.getCorrespondingMinuteData(obj1.data, obj1.dates, target));
//		 target = obj1.dates.get(16);
//		System.out.println(AddHourData.getCorrespondingMinuteData(obj1.data, obj1.dates, target));
//		 target = obj1.dates.get(17);
//		System.out.println(AddHourData.getCorrespondingMinuteData(obj1.data, obj1.dates, target));
//		target = obj1.dates.get(18);
//		System.out.println(AddHourData.getCorrespondingMinuteData(obj1.data, obj1.dates, target));
//		
		
		
		LstmModelPredictorImpl.getIndex(0, 45);
		
		
		
		
		
		
		
		
		fail("Not yet implemented");
	}

}
