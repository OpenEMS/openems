package io.openems.edge.controller.api.meteocontrol;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.MeteoControl", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeteoControl extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(MeteoControl.class);

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;

	@Reference
	protected ComponentManager componentManager;

	private Config config = null;

	private ZonedDateTime lasthour = null;

	// Required inverter values for MeteoControl
	private long e_int = 0; // Energy generated per interval
	private int p_dc = 0; // Power DC (single string or accumulated)
	private int u_dc = 0; // Voltage DC (single string or accumulated)
	// private int i_dc = 0; // Current DC (single string or accumulated)
	private int p_ac = 0; // Power AC (single phase or accumulated)
	// private int u_ac = 0; // Voltage AC (single phase or accumulated)
	// private int i_ac = 0; // Current AC (single phase or accumulated)

	// Additional inverter values for MeteoControl
	private int f_ac = 0; // Grid frequency
	private int q_ac = 0; // Reactive Power

	// Required meter values for MeteoControl
	// private long e_z_evu = 0; // Feed in energy meter

	// battery values for MeteoControl
	private int b_charge_level = 0; // SOC
	// private int b_capacity = 0; // Nominal capacity
	// private int b_u_dc = 0; // Battery voltage
	private int b_p_dc = 0; // Total battery power
	// private int b_i_dc = 0; // Battery Charge Current

	private String data = "";

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
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

	public MeteoControl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		if (this.testConnection()) {
			this.logInfo(this.log, "Connection Test Success!");
		} else {
			this.logError(this.log, "Connection Test Fail! Please check the configuration.");
		}
		this.lasthour = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS).minusHours(this.config.tInterval());

	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		ZonedDateTime now = ZonedDateTime.now();
		if (now.getMinute() > 14) {
			ZonedDateTime tmphour = now.truncatedTo(ChronoUnit.HOURS);

			if (tmphour.minusHours(this.config.tInterval()).equals(this.lasthour)) {

				this.collectData(now.truncatedTo(ChronoUnit.HOURS));
				this.logError(this.log, this.formatData());
				//this.sendData();

				this.lasthour = tmphour;
			}
		}

	}

	private boolean testConnection() {

		try {
			HttpResponse response = Request.Get(this.config.host() + "/api/public/connectiontest").execute()
					.returnResponse();
			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode == 200) {
				return true;
			} else {
				this.logError(this.log, "Connection Error! Got status Code: " + statusCode + ", reason:"
						+ response.getStatusLine().getReasonPhrase());
			}
		} catch (IOException e) {
			this.logError(this.log, e.getMessage());
			return false;
		}

		return false;
	}

	private void collectData(ZonedDateTime toDate) throws OpenemsNamedException {

		this.data = "";

		// Sum sum = this.componentManager.getComponent("_sum");
		SymmetricEss ess = this.componentManager.getComponent(this.config.essId());
		// ManagedSymmetricPvInverter pvInverter =
		// this.componentManager.getComponent(this.config.pvInverter());
		SymmetricMeter pvInverter = this.componentManager.getComponent(this.config.pvInverter());
		SymmetricMeter meter = this.componentManager.getComponent(this.config.meter());

		ChannelAddress e_intAddress = pvInverter.getActiveProductionEnergyChannel().address();
		ChannelAddress p_dcAddress = pvInverter.getActivePowerChannel().address();
		ChannelAddress u_dcAddress = pvInverter.getVoltageChannel().address();
		ChannelAddress p_acAddress = meter.getActivePowerChannel().address();
		ChannelAddress f_acAddress = meter.getFrequencyChannel().address();
		ChannelAddress q_acAddress = meter.getReactivePowerChannel().address();
		ChannelAddress b_charge_levelAddress = ess.getSocChannel().address();
		ChannelAddress b_p_dcAddress = ess.getActivePowerChannel().address();

		Set<ChannelAddress> eChannels = new HashSet<>();

		eChannels.add(e_intAddress);

		Set<ChannelAddress> channels = new HashSet<>();

		// channels.add(e_intAddress);
		channels.add(p_dcAddress);
		// channels.add(u_dcAddress);

		channels.add(p_acAddress);
		channels.add(f_acAddress);
		channels.add(q_acAddress);
		// channels.add(meter.getActiveProductionEnergy().address());

		channels.add(b_charge_levelAddress);
		channels.add(b_p_dcAddress);

		ZonedDateTime fromDate = toDate.minusHours(1);

		SortedMap<ChannelAddress, JsonElement> eHistory = this.getTimedata().queryHistoricEnergy(null, fromDate, toDate,
				eChannels);

		this.e_int = this.getHistoryDataValue(eHistory, e_intAddress);

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> history = this.getTimedata()
				.queryHistoricData(null, fromDate, toDate, channels, this.config.mInterval());
		for (ZonedDateTime t : history.keySet()) {

			SortedMap<ChannelAddress, JsonElement> tmp = history.get(t);
			String timestamp = t.truncatedTo(ChronoUnit.MINUTES)
					.format(DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss"));

			this.p_dc = this.getHistoryDataValue(tmp, p_dcAddress);
			this.u_dc = this.getHistoryDataValue(tmp, u_dcAddress);
			this.p_ac = this.getHistoryDataValue(tmp, p_acAddress);
			this.f_ac = this.getHistoryDataValue(tmp, f_acAddress);
			this.q_ac = this.getHistoryDataValue(tmp, q_acAddress);
			this.b_charge_level = this.getHistoryDataValue(tmp, b_charge_levelAddress);
			this.b_p_dc = this.getHistoryDataValue(tmp, b_p_dcAddress);

			this.data += timestamp + ";;" + this.config.serial() + ";" + this.config.mInterval() + ";" + e_int + ";"
					+ p_dc + ";" + u_dc + ";" + p_ac + ";" + f_ac + ";" + q_ac + ";" + b_charge_level + ";" + b_p_dc
					+ ";" + System.lineSeparator();

		}

	}

	private String formatData() {

		String offset = ZonedDateTime.now().getOffset().toString();
		JsonObject jsonData = new JsonObject();

		jsonData.addProperty("utcOffset", offset);
		jsonData.addProperty("intervall", this.config.mInterval());
		jsonData.addProperty("type", "inverter");

		String csvData = "timestamp;address;serial;interval;E_INT;P_DC;U_DC;P_AC;F_AC;Q_AC;B_CHARGE_LEVEL;B_P_DC;"
				+ System.lineSeparator();
		csvData += this.data;

		jsonData.addProperty("data", csvData);

		return jsonData.toString();
	}

	private Timedata getTimedata() throws OpenemsException {
		if (this.timedata != null) {
			return this.timedata;
		}
		throw new OpenemsException("There is no Timedata-Service available!");
	}

	private void sendData() throws OpenemsException {
		HttpPut httpPut = new HttpPut(this.config.host() + "/api/import/inverterdata");

		httpPut.setHeader("Accept", "application/json");
		httpPut.setHeader("Content-type", "application/json");
		httpPut.setHeader("Serial", this.config.serial());

		CredentialsProvider provider = new BasicCredentialsProvider();
		provider.setCredentials(AuthScope.ANY,
				new UsernamePasswordCredentials(this.config.user(), this.config.password()));

		CloseableHttpClient httpclient = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();

		try {
			CloseableHttpResponse response = httpclient.execute(httpPut);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new OpenemsException(response.getStatusLine().toString());
			}
		} catch (ClientProtocolException e) {

			e.printStackTrace();
			throw new OpenemsException(e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new OpenemsException(e.getMessage());
		}

	}

	private int getHistoryDataValue(SortedMap<ChannelAddress, JsonElement> history, ChannelAddress address) {

		int val = 0;
		try {
			val = history.get(address).getAsInt();
		} catch (Exception e) {
			// TODO Auto-generated catch block

		}
		return val;

	}
}
