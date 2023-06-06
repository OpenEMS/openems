package io.openems.edge.predictor.lstmmodel;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.predictor.api.oneday.AbstractPredictor24Hours;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;
import io.openems.edge.predictor.api.oneday.Predictor24Hours;
import io.openems.edge.predictor.lstmmodel.interpolation.LinearInterpolation;
import io.openems.edge.predictor.lstmmodel.interpolation.interpolationManager;
import io.openems.edge.predictor.lstmmodel.preprocessing.PreprocessingImpl;
import io.openems.edge.predictor.lstmmodel.util.Engine;
import io.openems.edge.predictor.lstmmodel.util.Engine.EngineBuilder;
import io.openems.edge.predictor.lstmmodel.util.makeMultipleModel;
import io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion;
import io.openems.edge.timedata.api.Timedata;

//import static io.openems.edge.predictor.lstmmodel.util.SlidingWindowSpliterator.windowed;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Predictor.LstmModel", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class LstmPredictorImpl extends AbstractPredictor24Hours implements Predictor24Hours, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(LstmPredictorImpl.class);

	public static final Function<List<Integer>, List<Double>> INTEGER_TO_DOUBLE_LIST = UtilityConversion::convertListIntegerToListDouble;
//
//	@Reference
//	private Timedata timedata;

	protected Config config;

	@Reference
	private ComponentManager componentManager;

	public LstmPredictorImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				LstmPredictor.ChannelId.values() //
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

//		var nowDate = ZonedDateTime.now(this.componentManager.getClock());
//		// From now time to Last 4 weeks
//		var fromDate = nowDate.minus(this.config.numOfWeeks(), ChronoUnit.WEEKS);
//
//		System.out.println("From date : " + fromDate //
//				+ "to current date : " + nowDate //
//				+ " equals to : " + zonedDateTimeDifference(fromDate, nowDate, ChronoUnit.DAYS) + " days");
//
//		final SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryResult;
//
//		// Query database
//		try {
//			queryResult = this.timedata.queryHistoricData(null, fromDate, nowDate, Sets.newHashSet(channelAddress),
//					new Resolution(15, ChronoUnit.MINUTES));
//		} catch (OpenemsNamedException e) {
//			this.logError(this.log, e.getMessage());
//			e.printStackTrace();
//			return Prediction24Hours.EMPTY;
//		}
//
//		// Extract data
//		List<Integer> data = queryResult.values().stream() //
//				.map(SortedMap::values) //
//				.flatMap(Collection::stream) //
//				.map(v -> {
//					if (v.isJsonNull()) {
//						return (Integer) null;
//					}
//					return v.getAsInt();
//				}).collect(Collectors.toList());
//
//		if (isAllNulls(data)) {
//			System.out.println("Data is all null, use different predictor");
//			return null;
//		}
//
//		System.out.println("adsfgsad" + data);
//
//		ArrayList<Double> doubleListData = new ArrayList<>();
//		
//		doubleListData = data.stream()//
//				.map(i -> i == null ? Double.NaN : i.doubleValue()) //
//				.collect(Collectors.toCollection(ArrayList::new));
//		
//		System.out.println(doubleListData);
//		/**
//		 * Computing interpolation as a combination of linear and cubical through interpolation manager. 
//		 */
//
//		interpolationManager interpolation = new interpolationManager(doubleListData);
//
//		doubleListData = interpolation.interpolated;
//		
//		/**
//		 * Grouping data by unique minutes in the data set using groupBy class
//		 */
//		/**
//		 * Grouping data by unique minutes in the data set using groupBy class
//		 */
//
//		System.out.println(data);
//
////		System.out.println("---");
////		System.out.println(data);
////		System.out.println("---");
////		System.out.println(INTEGER_TO_DOUBLE_LIST.apply(data));
////		System.out.println("---");
//
//		int windowsSize = 7;
//		PreprocessingImpl preprocessing = new PreprocessingImpl(doubleListData, windowsSize);
//
//		preprocessing.scale(0.2, 0.8);
//
//		double[] result;
//
//		try {
//			double[][] trainData = preprocessing.getFeatureData(preprocessing.trainTestSplit.trainIndexLower,
//					preprocessing.trainTestSplit.trainIndexHigher);
//
//			double[][] validateData = preprocessing.getFeatureData(preprocessing.trainTestSplit.validateIndexLower,
//					preprocessing.trainTestSplit.validateIndexHigher);
//
//			double[][] testData = preprocessing.getFeatureData(preprocessing.trainTestSplit.testIndexLower,
//					preprocessing.trainTestSplit.testIndexHigher);
//
//			double[] trainTarget = preprocessing.getTargetData(preprocessing.trainTestSplit.trainIndexLower,
//					preprocessing.trainTestSplit.trainIndexHigher);
//
//			double[] validateTarget = preprocessing.getTargetData(preprocessing.trainTestSplit.validateIndexLower,
//					preprocessing.trainTestSplit.validateIndexHigher);
//
//			System.out.println("Train Window size   : " + trainData[0].length);
//			System.out.println("Train No of windows : " + trainData.length);
//			System.out.println("Train target Size   : " + trainTarget.length);
//
//			System.out.println("Validate Window size   : " + validateData[0].length);
//			System.out.println("Validate No of windows : " + validateData.length);
//			System.out.println("Validate target Size   : " + validateTarget.length);
//
//			System.out.println("Test Window size   : " + testData[0].length);
//			System.out.println("Test No of windows : " + testData.length);
//
//			Engine model = new EngineBuilder() //
//					.setInputMatrix(trainData) //
//					.setTargetVector(trainTarget) //
//					.setValidateData(validateData) //
//					.setValidateTarget(validateTarget) //
//					.build();
//
//			int epochs = 1000;
//			model.fit(epochs);
//			
//			//model.finalWeight
//
//			result = model.predict(testData);
//
//			Integer[] perdictedValues = preprocessing.reverseScale(0.2, 0.8, result);
//
//			return new Prediction24Hours(perdictedValues);
//
//		} catch (Exception e) {
//			e.printStackTrace();
//			return null;
//		}
		
		
		//x[][] = makeMultipleModel();
		 makeMultipleModel models = new makeMultipleModel();
		
		return null;

	}

	/**
	 * Does the array is all nulls or not.
	 * 
	 * @param array the array
	 * @return True if all elements are null, false otherwise
	 */
	public static boolean isAllNulls(Iterable<?> array) {
		return StreamSupport //
				.stream(array.spliterator(), true) //
				.allMatch(o -> o == null);
	}

	/**
	 * Gets the number of days from fromDate and nowDate.
	 * 
	 * @param fromDate ZonedDateTime fromDate
	 * @param nowDate  ZonedDateTime nowDate
	 * @param unit     ChronoUnit.day
	 * @return Number of days from fromDate and nowDate
	 */
	static long zonedDateTimeDifference(ZonedDateTime fromDate, ZonedDateTime nowDate, ChronoUnit unit) {
		return unit.between(fromDate, nowDate);
	}

}
