package io.openems.edge.predictor.lstm.predictor;

import java.awt.Color;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;

import io.openems.edge.common.test.Plot;
import io.openems.edge.common.test.Plot.AxisFormat;
import io.openems.edge.predictor.lstm.common.DataModification;
import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.common.ReadModels;
import io.openems.edge.predictor.lstm.interpolation.InterpolationManager;
import io.openems.edge.predictor.lstm.preprocessing.GroupBy;

public class Prediction {
	private ArrayList<Double> predictedAndScaledBack = new ArrayList<Double>();
	private ArrayList<Double> predicted = new ArrayList<Double>();

	public Prediction(ArrayList<Double> data, ArrayList<OffsetDateTime> date, String path,
			HyperParameters hyperParameters) {
		ArrayList<ArrayList<ArrayList<OffsetDateTime>>> dateGroupedByMinute = new ArrayList<ArrayList<ArrayList<OffsetDateTime>>>();
		ArrayList<ArrayList<Double>> dataGroupedByMinute1 = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<ArrayList<Double>>> dataGroupedByMinute = new ArrayList<ArrayList<ArrayList<Double>>>();
		ArrayList<Double> dataToPredict = data;
		final ArrayList<OffsetDateTime> dateToPredict = date;	
		// Interpolate
		InterpolationManager interpolationManager = new InterpolationManager(dataToPredict, dateToPredict,hyperParameters);
		dataToPredict = interpolationManager.getInterpolatedData();
		// Scaling
		ArrayList<Double> scaledData = DataModification.scale(dataToPredict, hyperParameters.getScalingMin(),
				hyperParameters.getScalingMax());
		// Grouping data by hour
		GroupBy groupBy = new GroupBy(scaledData, dateToPredict);
		groupBy.hour();
		// Grouping data by minute
		for (int i = 0; i < groupBy.getDateGroupedByHour().size(); i++) {
			GroupBy gB = new GroupBy(groupBy.getDataGroupedByHour().get(i), groupBy.getDateGroupedByHour().get(i));
			gB.minute();
			dataGroupedByMinute.add(gB.getDataGroupedByMinute());
			dateGroupedByMinute.add(gB.getDateGroupedByMinute());
		}

		for (int i = 0; i < dataGroupedByMinute.size(); i++) {
			for (int j = 0; j < dataGroupedByMinute.get(i).size(); j++) {
				dataGroupedByMinute1.add(dataGroupedByMinute.get(i).get(j));
			}
		}

		// Make prediction
		ArrayList<ArrayList<ArrayList<Double>>> allModel = ReadModels.getModelForSeasonality(path, hyperParameters)
				.get(0);

		this.predicted = Predictor.predictPre(dataGroupedByMinute1, allModel);
		for (int i = 0; i < this.predicted.size(); i++) {
			this.predictedAndScaledBack.add(DataModification.scaleBack(this.predicted.get(i),
					hyperParameters.getScalingMin(), hyperParameters.getScalingMax()));
		}
	}

	/**
	 * Generate and save a plot to visualize predicted and actual values for a given
	 * week. This method generates a plot to visualize predicted and actual values
	 * for a specific week. The plot displays the actual and predicted values for
	 * each 15-minute interval in a day.
	 *
	 * @param predictedValues An ArrayList of Double values representing the
	 *                        predicted values.
	 * @param orginal         An ArrayList of Double values representing the
	 *                        original (actual) values.
	 * @param weekNumber      An integer representing the week number for which the
	 *                        plot is generated.
	 * @param hyperParameters Is the object of class HyperParameters.
	 * 
	 */

	public static void makePlot(ArrayList<Double> predictedValues, ArrayList<Double> orginal,
			HyperParameters hyperParameters, int weekNumber) {
		Plot.Data dataActualValues = Plot.data();
		Plot.Data dataPredictedValues = Plot.data();

		for (int i = 0; i < 60 / hyperParameters.getInterval() * 24; i++) {
			dataActualValues.xy(i, predictedValues.get(i));
			dataPredictedValues.xy(i, orginal.get(i));
		}

		Plot plot = Plot.plot(//
				Plot.plotOpts() //
						.title("Prediction Charts for week " + weekNumber) //
						.legend(Plot.LegendFormat.BOTTOM)) //
				.xAxis("x, every 15min data for a day", Plot.axisOpts() //
						.format(AxisFormat.NUMBER_INT) //
						.range(0, 96)) //
				.yAxis("y, Kilo Watts ", Plot.axisOpts() //
						.format(AxisFormat.NUMBER_INT)) //
				.series("Actual", dataActualValues, Plot.seriesOpts() //
						.color(Color.BLACK)) //
				.series("Prediction", dataPredictedValues, Plot.seriesOpts() //
						.color(Color.RED)); //

		try {
			
			String path = "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder";
			plot.save(path + "/prediction", "png");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public ArrayList<Double> getPredictedValues() {
		return this.predictedAndScaledBack;
	}

}
