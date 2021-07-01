package io.openems.edge.predictor.weka;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import weka.classifiers.evaluation.NumericPrediction;
import weka.classifiers.functions.SMOreg;
import weka.classifiers.timeseries.WekaForecaster;
import weka.core.Instances;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Predictor.Weka", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class WekaPredictorImpl extends AbstractOpenemsComponent implements WekaPredictor, OpenemsComponent {

	private Config config = null;

	public WekaPredictorImpl() {
		super(//
				OpenemsComponent.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		predict();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private static void predict() {
		try {
			// path
			String pathToWindData = "C:\\Users\\stefan.feilmeier\\wine.arff";

			// load the data
			Instances wind = new Instances(new BufferedReader(new FileReader(pathToWindData)));

			// new forecaster
			WekaForecaster forecaster = new WekaForecaster();

			// set the targets we want to forecast. This method calls
			// setFieldsToLag() on the lag maker object for us
//			forecaster.setFieldsToForecast("capacity (kW)");
			forecaster.setFieldsToForecast("Fortified,Dry-white");

			// default underlying classifier is SMOreg (SVM)
			forecaster.setBaseForecaster(new SMOreg());

			forecaster.getTSLagMaker().setTimeStampField("Date"); // date time stamp
			forecaster.getTSLagMaker().setMinLag(1);
			forecaster.getTSLagMaker().setMaxLag(3); // monthly data

			// add a month of the year indicator field
			// forecaster.getTSLagMaker().setAddMonthOfYear(true);

			// add a quarter of the year indicator field
			// forecaster.getTSLagMaker().setAddQuarterOfYear(true);

			// build the model
			forecaster.buildForecaster(wind, System.out);

			// prime the forecaster with enough recent historical data
			// to cover up to the maximum lag.
			forecaster.primeForecaster(wind);

			// forecast for 2 days
			List<List<NumericPrediction>> forecast = forecaster.forecast(192, System.out);

			// output the predictions. Outer list is over the steps; inner list is over
			// the targets
			for (int i = 0; i < 192; i++) {
				List<NumericPrediction> predsAtStep = forecast.get(i);
				for (int j = 0; j < 1; j++) {
					NumericPrediction predForTarget = predsAtStep.get(j);
					System.out.print("" + predForTarget.predicted() + " ");
				}
				System.out.println();
			}

			// we can continue to use the trained forecaster for further forecasting
			// by priming with the most recent historical data (as it becomes available).
			// At some stage it becomes prudent to re-build the model using current
			// historical data.

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
