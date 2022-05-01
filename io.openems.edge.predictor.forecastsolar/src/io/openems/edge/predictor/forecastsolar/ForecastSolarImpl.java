package io.openems.edge.predictor.forecastsolar;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.DateUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.linecharacteristic.PolyLine;
import io.openems.edge.common.type.CircularTreeMap;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;
import io.openems.edge.predictor.api.oneday.Predictor24Hours;
import okhttp3.OkHttpClient;
import okhttp3.Request;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Predictor.ForecastSolar", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ForecastSolarImpl extends AbstractOpenemsComponent
		implements ForecastSolar, Predictor24Hours, OpenemsComponent {

	private final CircularTreeMap<ZonedDateTime, PolyLine> historicPredictions = new CircularTreeMap<>(24);

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private Future<?> future;

	private ChannelAddress[] channelAddresses;
	private String url;
	private PolyLine prediction = PolyLine.empty();

	@Reference
	private ComponentManager componentManager;

	public ForecastSolarImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ForecastSolar.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.updateConfig(config);
		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		this.updateConfig(config);
		super.modified(context, config.id(), config.alias(), config.enabled());
	}

	private void updateConfig(Config config) throws OpenemsNamedException {
		this.channelAddresses = new ChannelAddress[] { ChannelAddress.fromString(config.channelAddress()) };
		this.url = "https://api.forecast.solar" //
				+ (config.apikey() == null || config.apikey().isBlank() ? "" : config.apikey()) //
				+ "/estimate/watts" //
				+ "/" + config.latitude() //
				+ "/" + config.longitude() //
				+ "/" + config.declination() //
				+ "/" + config.azimuth() //
				+ "/" + config.modulesPower() //
				+ "?time=utc";

		// run and schedule worker thread
		var future = this.future;
		if (future != null) {
			future.cancel(true);
		}
		this.future = this.executor.submit(this.task);
	}

	private final Runnable task = () -> {
		var now = ZonedDateTime.now();

		// Store old prediction
		var nowRoundedDownToHour = now.truncatedTo(ChronoUnit.HOURS);
		this.historicPredictions.put(nowRoundedDownToHour, prediction);

		// Update prediction
		this.prediction = updatePrediction(this.url);

		// Plan next run on next full hour
		var delay = now.until(now.plusHours(1).truncatedTo(ChronoUnit.HOURS), ChronoUnit.MILLIS);
		this.future = this.executor.schedule(this.task, delay, TimeUnit.MILLISECONDS);

		// Update Channels
		try {
			this.updateChannel(ForecastSolar.ChannelId.PREDICTED_18H_AGO, 18);
			this.updateChannel(ForecastSolar.ChannelId.PREDICTED_14H_AGO, 14);
			this.updateChannel(ForecastSolar.ChannelId.PREDICTED_10H_AGO, 10);
			this.updateChannel(ForecastSolar.ChannelId.PREDICTED_6H_AGO, 6);
			this.updateChannel(ForecastSolar.ChannelId.PREDICTED_2H_AGO, 2);
			this.channel(ForecastSolar.ChannelId.ACTUAL).setNextValue(//
					this.componentManager.getChannel(this.channelAddresses[0]).value());
		} catch (IllegalArgumentException | OpenemsNamedException e) {
			e.printStackTrace();
		}
	};

	private void updateChannel(ForecastSolar.ChannelId channelId, int hoursAgo) {
		var now = ZonedDateTime.now();
		var historicPrediction = this.historicPredictions.get(now.truncatedTo(ChronoUnit.HOURS).minusHours(hoursAgo));
		final Integer value;
		if (historicPrediction == null) {
			value = null;
		} else {
			value = TypeUtils.getAsType(OpenemsType.INTEGER,
					historicPrediction.getValue(Double.valueOf(now.toEpochSecond())));
		}
		this.channel(channelId).setNextValue(value);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private static PolyLine updatePrediction(String url) {
		var result = PolyLine.create();
		var client = new OkHttpClient();
		var request = new Request.Builder() //
				.url(url) //
				.build();
		try (var response = client.newCall(request).execute()) {

			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response);
			}

			var line = JsonUtils.parseToJsonObject(response.body().string());
			var data = JsonUtils.getAsJsonObject(line, "result");

			for (var entry : data.entrySet()) {
				var time = ZonedDateTime.parse(entry.getKey()).withZoneSameInstant(ZoneId.systemDefault());
				var value = JsonUtils.getAsInt(entry.getValue());
				result.addPoint(time.toEpochSecond(), value);
			}

		} catch (IOException | OpenemsNamedException e) {
			e.printStackTrace();
		}
		return result.build();
	}

	@Override
	public ChannelAddress[] getChannelAddresses() {
		return this.channelAddresses;
	}

	@Override
	public Prediction24Hours get24HoursPrediction(ChannelAddress channelAddress) {
		var start = DateUtils.roundZonedDateTimeDownToMinutes(ZonedDateTime.now(), 15).toEpochSecond();

		var result = new Integer[96];
		for (int i = 0; i < 96; i++) {
			var epoch = start + i * 15 * 60;
			var value = this.prediction.getValue(Double.valueOf(epoch));
			result[i] = TypeUtils.getAsType(OpenemsType.INTEGER, value);
		}
		return new Prediction24Hours(result);
	}
}
