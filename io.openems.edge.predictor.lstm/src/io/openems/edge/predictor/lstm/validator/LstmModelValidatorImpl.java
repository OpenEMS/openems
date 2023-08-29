package io.openems.edge.predictor.lstm.validator;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.predictor.api.oneday.AbstractPredictor24Hours;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;
import io.openems.edge.predictor.api.oneday.Predictor24Hours;
import io.openems.edge.predictor.lstm.utilities.UtilityConversion;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Lstm.Model.validator", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class LstmModelValidatorImpl extends AbstractPredictor24Hours
		implements Predictor24Hours, OpenemsComponent /* , org.osgi.service.event.EventHandler */ {

	// private final Logger log = LoggerFactory.getLogger(LstmPredictorImpl.class);

	public static final Function<List<Integer>, List<Double>> INTEGER_TO_DOUBLE_LIST = UtilityConversion::convertListIntegerToListDouble;

	@Reference
	private Timedata timedata;

	protected Config config;

	@Reference
	private ComponentManager componentManager;

	public LstmModelValidatorImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values() //
				

		);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		super.activate(context, this.config.id(), this.config.alias(), this.config.enabled(),
				this.config.channelAddresses());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ClockProvider getClockProvider() {
		return this.componentManager;
	}

	@Override
	protected Prediction24Hours createNewPrediction(ChannelAddress channelAddress) {

//		/// var nowDate = ZonedDateTime.now(this.componentManager.getClock());
//		// From now time to Last 4 weeks
//		// var fromDate = nowDate.minus(this.config.numOfWeeks(), ChronoUnit.WEEKS);
//
//		// TODO change the logic for date range
//
//		// ZonedDateTime nowDate = ZonedDateTime.now();
//		ZonedDateTime nowDate = ZonedDateTime.of(2023, 6, 24, 0, 0, 0, 0, ZonedDateTime.now().getZone());
//		ZonedDateTime till = ZonedDateTime.of(nowDate.getYear(), nowDate.getMonthValue(), //
//				nowDate.minusDays(1).getDayOfMonth(), 11, 45, 0, 0, nowDate.getZone());
//		ZonedDateTime temp = till.minusDays(6);
//		ZonedDateTime fromDate = ZonedDateTime.of(temp.getYear(), temp.getMonthValue(), temp.getDayOfMonth(), 0, 0, 0,
//				0, //
//				temp.getZone());
//
//		System.out.println("From : " + fromDate);
//
//		System.out.println("Till : " + till);
//
//		// TEMP
//
//		// Extract data
//
//		DataQuerry predictionData = new DataQuerry(fromDate, nowDate, 15, timedata);
//
//		// get date
//
//		System.out.println(predictionData.date.size());
//
//		// data conversion
//		// make 96datepoint prediction
//		int minOfTrainingData = 33246;
//		int maxOfTrainingData = 73953495;
//
//		Validation obj = new Validation((ArrayList<Double>) predictionData.data, predictionData.date, minOfTrainingData,
//				maxOfTrainingData);

		return null;

	}

//	@Override
//	  public void handleEvent(Event event) {
//        if (!this.isEnabled()) {
//            return;
//        }
//        switch (event.getTopic()) {
//       
//        }
//    }

}
