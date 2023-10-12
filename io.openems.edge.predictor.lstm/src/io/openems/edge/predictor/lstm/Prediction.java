package io.openems.edge.predictor.lstm;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;

import io.openems.edge.predictor.lstm.common.DataModification;
import io.openems.edge.predictor.lstm.common.ReadModels;
import io.openems.edge.predictor.lstm.interpolation.InterpolationManager;
import io.openems.edge.predictor.lstm.preprocessing.GroupBy;

//CHECKSTYLE:OFF
public class Prediction {
	public ArrayList<Double> predictedAndScaledBack = new ArrayList<Double>();
	public ArrayList<Double> dataShouldBe = new ArrayList<Double>();
	public ArrayList<Double> predicted = new ArrayList<Double>();
	public double min = 0;
	public double max = 0;

	public Prediction(ArrayList<Double> data, ArrayList<OffsetDateTime> date, double min, double max) {

		ArrayList<ArrayList<ArrayList<OffsetDateTime>>> dateGroupedByMinute = new ArrayList<ArrayList<ArrayList<OffsetDateTime>>>();
		ArrayList<ArrayList<Double>> dataGroupedByMinute1 = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<ArrayList<Double>>> dataGroupedByMinute = new ArrayList<ArrayList<ArrayList<Double>>>();
		ArrayList<Double> scaledData = new ArrayList<Double>();

		ArrayList<Double> dataToPredict = data;
		ArrayList<OffsetDateTime> dateToPredict = date;

		min = Collections.min(data);
		max = Collections.max(data);

		// Interpolate
		InterpolationManager interpolationManager = new InterpolationManager(dataToPredict);
		dataToPredict = interpolationManager.interpolated;

		// Scaling

		scaledData = DataModification.scale(dataToPredict, min, max);

		// Grouping data by hour
		GroupBy groupBy = new GroupBy(scaledData, dateToPredict);
		groupBy.hour();

		// Grouping data by minute
		for (int i = 0; i < groupBy.getGroupedDataByHour().size() /* groupedDataByHour.size() */; i++) {
			GroupBy gB = new GroupBy(groupBy.getGroupedDataByHour().get(i) /* groupedDataByHour.get(i) */,
					groupBy.getGroupedDateByHour().get(i) /* groupedDateByHour.get(i) */);
			gB.minute();
			dataGroupedByMinute.add(gB.getGroupedDataByMin() /* groupedDataByMin */);
			dateGroupedByMinute.add(gB.getGroupedDateByMin() /* groupedDateByMin */);
		}

		for (int i = 0; i < dataGroupedByMinute.size(); i++) {
			for (int j = 0; j < dataGroupedByMinute.get(i).size(); j++) {
				dataGroupedByMinute1.add(dataGroupedByMinute.get(i).get(j));
			}
		}

		ReadModels readModels = new ReadModels();

		// Make prediction
		predicted = Predictor.Predict(dataGroupedByMinute1, readModels.allModel.get(readModels.allModel.size() - 1));

		for (int i = 0; i < predicted.size(); i++) {
			predictedAndScaledBack.add(DataModification.scaleBack(predicted.get(i), min, max));
		}
	}

//	public static void makePlot(ArrayList<Double> predictedValues, ArrayList<Double> orginal, int weekNumber) {
//		Plot.Data dataActualValues = Plot.data();
//		Plot.Data dataPredictedValues = Plot.data();
//
//		for (int i = 0; i < 96; i++) {
//			dataActualValues.xy(i, predictedValues.get(i));
//			dataPredictedValues.xy(i, orginal.get(i));
//		}
//
//		Plot plot = Plot.plot(//
//				Plot.plotOpts() //
//						.title("Prediction Charts for week " + weekNumber) //
//						.legend(Plot.LegendFormat.BOTTOM)) //
//				.xAxis("x, every 15min data for a day", Plot.axisOpts() //
//						.format(AxisFormat.NUMBER_INT) //
//						.range(0, 96)) //
//				.yAxis("y, Kilo Watts ", Plot.axisOpts() //
//						.format(AxisFormat.NUMBER_INT)) //
//				.series("Actual", dataActualValues, Plot.seriesOpts() //
//						.color(Color.BLACK)) //
//				.series("Prediction", dataPredictedValues, Plot.seriesOpts() //
//						.color(Color.RED)); //
//
//		try {
//			String path = "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstmmodel\\testResults";
//			plot.save(path + "/prediction", "png");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//	}
	// CHECKSTYLE:ON
}
