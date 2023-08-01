package io.openems.edge.predictor.lstmmodel;

import java.awt.Color;
import java.io.IOException;

import org.junit.Test;
//import io.openems.edge.predictor.lstmmodel.validation.ValidationUpdated;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.Plot;
import io.openems.edge.common.test.Plot.AxisFormat;

public class LstmModelPredictorTest {

	private static final String TIMEDATA_ID = "timedata0";
	private static final String PREDICTOR_ID = "predictor0";

	private static final ChannelAddress METER1_ACTIVE_POWER = new ChannelAddress("meter1", "ActivePower");
//
	@Test
	public void test() throws Exception {
		
		
		//makeMultipleModel obj = new makeMultipleModel();
		//Validation obj1 = new Validation();
		 //Prediction obj2 = new Prediction(33246,73953495);
		//LstmPredictorImpl obj5 = new LstmPredictorImpl ();
	
		
//
//	final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */,
//				ZoneOffset.UTC);
//
//	     var values = Data.data;
		
		
//
//		System.out.println("length of the Arrayy : " + values.length);
//		var predictedValues = Data.actualData;
//
//		var timedata = ne
//		w DummyTimedata(TIMEDATA_ID);
//		var start = ZonedDateTime.of(2019, 12, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
//
//		for (var i = 0; i < values.length; i++) {
//			timedata.add(start.plusMinutes(i * 15), METER1_ACTIVE_POWER, values[i]);
//		}
//
//		var sut = new LstmPredictorImpl()
//
//		new ComponentTest(sut) //
//				.addReference("timedata", timedata) //
//				.addReference("componentManager", new DummyComponentManager(clock)) //
//				.activate(MyConfig.create() //
//						.setId(PREDICTOR_ID) //
//						.setNumOfWeeks(4) //
//						.setChannelAddresses(METER1_ACTIVE_POWER.toString()).build());
//
//		var prediction = sut.get24HoursPrediction(METER1_ACTIVE_POWER);
//		var p = prediction.getValues();
//
////		assertEquals(predictedValues[0], p[0]);
////		assertEquals(predictedValues[48], p[48]);
////		assertEquals(predictedValues[95], p[95]);
//
//		System.out.println(Arrays.toString(prediction.getValues()));
//
//		// plotting
//		//makePlot(predictedValues, p);
//
	}
//
	private void makePlot(Integer[] predictedValues, Integer[] p) {
		Plot.Data dataActualValues = Plot.data();
		Plot.Data dataPredictedValues = Plot.data();

		for (int i = 0; i < 96; i++) {
			dataActualValues.xy(i, predictedValues[i]);
			dataPredictedValues.xy(i, p[i]);
		}

		Plot plot = Plot.plot(//
				Plot.plotOpts() //
						.title("Pridction Charts") //
						.legend(Plot.LegendFormat.BOTTOM)) //
				.xAxis("x, every 15min data for a day", Plot.axisOpts() //
						.format(AxisFormat.NUMBER_INT) //
						.range(0, 96)) //
				.yAxis("y, Watts ", Plot.axisOpts() //
						.format(AxisFormat.NUMBER_INT)) //
				.series("Actual", dataActualValues, Plot.seriesOpts() //
						.color(Color.BLACK)) //
				.series("Prediction", dataPredictedValues, Plot.seriesOpts() //
						.color(Color.RED)); //

		try {
			String path = "./testResults";
			plot.save(path + "/prediction", "png");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	
//
}
