package io.openems.edge.predictor.lstmmodel;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.predictor.lstmmodel.predictor.DataQuarry;

import static org.junit.Assert.*;

import java.time.ZonedDateTime;

import org.junit.Test;

public class DataQuarryTest {

	@Test
	public void test() throws OpenemsNamedException {
		ZonedDateTime nowDate = ZonedDateTime.of(2023,6,7,0,0,0,0,ZonedDateTime.now().getZone());
		ZonedDateTime till = ZonedDateTime.of(nowDate.getYear(), nowDate.getMonthValue(),//
		nowDate.minusDays(1).getDayOfMonth(), 11, 45, 0, 0, nowDate.getZone());
		ZonedDateTime temp = till.minusDays(6);
		ZonedDateTime fromDate = ZonedDateTime.of(temp.getYear(), temp.getMonthValue(), temp.getDayOfMonth(), 0, 0, 0, 0,//
		temp.getZone());
		System.out.println(fromDate);
		System.out.println(till);
		
		//DataQuarry predictionData = new  DataQuarry(fromDate, nowDate,15);
		//System.out.println(predictionData);
	}

}
