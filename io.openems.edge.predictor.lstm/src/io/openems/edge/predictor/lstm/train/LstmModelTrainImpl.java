package io.openems.edge.predictor.lstm.train;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Lstm.Model.train", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class LstmModelTrainImpl extends AbstractOpenemsComponent
		implements OpenemsComponent /* , org.osgi.service.event.EventHandler */ {

	// private final Logger log = LoggerFactory.getLogger(LstmPredictorImpl.class);

	public static final Function<List<Integer>, List<Double>> INTEGER_TO_DOUBLE_LIST = io.openems.edge.predictor.lstm.utilities.UtilityConversion::convertListIntegerToListDouble;

	@Reference
	private Timedata timedata;

	protected Config config;

	@Reference
	private ComponentManager componentManager;

	public LstmModelTrainImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values() //

		);
	}

	ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		super.activate(context, this.config.id(), this.config.alias(), this.config.enabled());

		scheduler.scheduleAtFixedRate(//
				new LstmTrain(this.timedata, config.channelAddresses()), //
				0, 15, TimeUnit.DAYS);

	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.scheduler.shutdown();
		super.deactivate();
	}

	protected void train(ChannelAddress channelAddress) {

		HyperParameters hyperParameters = HyperParameters.getInstance();

		// This is reference date specific to fems
		ZonedDateTime nowDate = ZonedDateTime.of(2023, 10, 01, 0, 0, 0, 0, ZonedDateTime.now().getZone());
		ZonedDateTime until = ZonedDateTime.of(//
				nowDate.getYear(), //
				nowDate.getMonthValue(), //
				nowDate.minusDays(1).getDayOfMonth(), //
				23, //
				45, //
				0, //
				0, //
				nowDate.getZone());
		System.out.println("Now Date : " + nowDate);
		ZonedDateTime temp = until.minusDays(30);

		ZonedDateTime fromDate = ZonedDateTime.of(//
				temp.getYear(), //
				temp.getMonthValue(), //
				temp.getDayOfMonth(), //
				0, //
				0, //
				0, //
				0, //
				ZonedDateTime.now().getZone());
		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> querryResult = new TreeMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>();
		try {
			querryResult = this.timedata.queryHistoricData(null, fromDate, until, Sets.newHashSet(channelAddress),
					new Resolution(hyperParameters.getInterval(), ChronoUnit.MINUTES));
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
		// ArrayList<Double> data = this.getData(querryResult);
		// ArrayList<OffsetDateTime> date = this.getDate(querryResult);
		System.out.println("....Training.....");

		// Reading CSV file and looping over 26 fems data
		// int k = 0;
		// for(int i = 0;i<= 26;i++) {
		//
		// k= hyperParameters.getCount();
		//
		//
		// String pathValidate = Integer.toString(27) + ".csv";
		// String pathTrain = Integer.toString(i+1) + ".csv";
		// ReadCsv obj2 = new ReadCsv(pathValidate);
		// ReadCsv obj1 = new ReadCsv(pathTrain);
		// // LstmTrain obj3 = new LstmTrain();
		// new MultiThreadTrain(obj1.getData(), obj1.getDates(), obj2.getData(),
		// obj2.getDates(), hyperParameters);
		// }

	}

	/**
	 * Extracts Double values from a sorted map of ZonedDateTime keys and associated
	 * JsonElement values.
	 *
	 * @param querryResult A sorted map containing ZonedDateTime keys and associated
	 *                     data in the form of JsonElement objects.
	 * @return An ArrayList of Double values extracted from the JsonElement objects
	 *         in querryResult.
	 */

	public ArrayList<Double> getData(SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> querryResult) {

		ArrayList<Double> data = (ArrayList<Double>) querryResult.values().stream() //
				.map(SortedMap::values) //
				.flatMap(Collection::stream) //
				.map(v -> {
					if (v.isJsonNull()) {
						return null;
					}
					return v.getAsDouble();
				}).collect(Collectors.toList());

		if (this.isAllNulls(data)) {
			System.out.println("Data is all null, use different predictor");
		}
		return data;
	}

	/**
	 * Extracts OffsetDateTime objects from a sorted map of ZonedDateTime keys.
	 *
	 * @param querryResult A sorted map containing ZonedDateTime keys and associated
	 *                     data.
	 * @return An ArrayList of OffsetDateTime objects extracted from the keys of
	 *         querryResult.
	 */

	public ArrayList<OffsetDateTime> getDate(
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> querryResult) {

		ArrayList<OffsetDateTime> date = new ArrayList<OffsetDateTime>();

		querryResult.keySet()//
				.stream()//
				.forEach(zonedDateTime -> {
					date.add(zonedDateTime.toOffsetDateTime());
				});
		return date;
	}

	private boolean isAllNulls(ArrayList<Double> array) {
		return StreamSupport //
				.stream(array.spliterator(), true) //
				.allMatch(o -> o == null);
	}
}
