package io.openems.edge.energytariff.corrently;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "EnergyTariff.Corrently", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Corrently extends AbstractOpenemsComponent implements OpenemsComponent {

	private final static String BASE_URL = "https://api.corrently.io";
	private final static String URL_BEST_HOUR = BASE_URL + "/gsi/bestHour";

	private final Logger log = LoggerFactory.getLogger(Corrently.class);

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		BEST_HOUR_GSI(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT)), //
		BEST_HOUR_EPOCHTIME(Doc.of(OpenemsType.LONG)), //

		REST_API_FAILED(Doc.of(Level.FAULT)), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> future = null;

	public Corrently() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws UnknownHostException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!config.enabled()) {
			return;
		}

		/*
		 * Define the 'worker' that gets the data from corrently Api.
		 */
		final Runnable worker = () -> {
			try {
				URL url = new URL(URL_BEST_HOUR + "?zip=" + URLEncoder.encode(config.zipCode(), "UTF-8"));
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setRequestMethod("GET");
				con.setConnectTimeout(5000);
				con.setReadTimeout(5000);
				int status = con.getResponseCode();
				if (status < 300) {
					try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
						// Read HTTP response
						StringBuilder content = new StringBuilder();
						String line;
						while ((line = in.readLine()) != null) {
							content.append(line);
							content.append(System.lineSeparator());
						}

						// Parse response to JSON
						JsonObject response = JsonUtils.parseToJsonObject(content.toString());
						JsonElement data = JsonUtils.getAsJsonArray(response, "data").get(0);

						// Parse values and set Channels
						long epochtime = JsonUtils.getAsLong(data, "epochtime");
						int gsi = JsonUtils.getAsInt(data, "gsi");
						this.setChannels(epochtime, gsi, false);
					}
				}
			} catch (OpenemsNamedException | IOException e) {
				this.logError(this.log, "Unable to read from Corrently API: " + e.getMessage());
				this.setChannels(null, null, true);
			}
		};

		/*
		 * Execute the 'worker' now and once every full hour.
		 */
		// execute now
		this.scheduler.execute(worker);

		// execute every full hour
		long secondsTillFullHour = LocalDateTime.now()
				.until(LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0), ChronoUnit.SECONDS);
		this.future = this.scheduler.scheduleAtFixedRate(//
				worker, //
				secondsTillFullHour, //
				60 /* seconds */ * 60 /* minutes */, //
				TimeUnit.SECONDS);
	}

	@Deactivate
	protected void deactivate() {
		if (this.future != null) {
			this.future.cancel(true);
		}
		this.scheduler.shutdown();
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return "Best Hour:" //
				+ this.channel(ChannelId.BEST_HOUR_EPOCHTIME).value().asString() //
				+ " with GSI=" + this.channel(ChannelId.BEST_HOUR_GSI).value().asString();
	}

	private void setChannels(Long epochtime, Integer gsi, boolean failed) {
		this.channel(ChannelId.BEST_HOUR_EPOCHTIME).setNextValue(epochtime);
		this.channel(ChannelId.BEST_HOUR_GSI).setNextValue(gsi);
		this.channel(ChannelId.REST_API_FAILED).setNextValue(failed);
	}
}
