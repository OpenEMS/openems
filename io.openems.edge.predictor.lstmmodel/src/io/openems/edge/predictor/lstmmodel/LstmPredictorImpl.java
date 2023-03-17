package io.openems.edge.predictor.lstmmodel;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.stream.Collectors;

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
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Predictor.LstmModel", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class LstmPredictorImpl extends AbstractPredictor24Hours implements Predictor24Hours, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(LstmPredictorImpl.class);

	@Reference
	private Timedata timedata;

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

		var now = ZonedDateTime.now(this.componentManager.getClock());
		// From now time to Last 4 weeks
		var fromDate = now.minus(this.config.numOfWeeks(), ChronoUnit.WEEKS);

		final SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryResult;

		// Query database
		try {
			queryResult = this.timedata.queryHistoricData(null, fromDate, now, Sets.newHashSet(channelAddress),
					new Resolution(15, ChronoUnit.MINUTES));
		} catch (OpenemsNamedException e) {
			this.logError(this.log, e.getMessage());
			e.printStackTrace();
			return Prediction24Hours.EMPTY;
		}

		// Extract data
		List<Integer> data = queryResult.values().stream() //
				.map(SortedMap::values) //
				// extract JsonElement values as flat stream
				.flatMap(Collection::stream) //
				// convert JsonElement to Integer
				.map(v -> {
					if (v.isJsonNull()) {
						return (Integer) null;
					}
					return v.getAsInt();
				})
				// get as Array
				.collect(Collectors.toList());

		// Num of Data per day
		// TODO change this variable based on the resolution which is 900 in query
		var numOfDataPerDay = 96;
		
		// LSTM model 
		
		
		
		
		// Return LSTM result
		

		return null;
	}

}
