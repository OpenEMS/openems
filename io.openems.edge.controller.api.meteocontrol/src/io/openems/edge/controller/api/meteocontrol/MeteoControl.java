package io.openems.edge.controller.api.meteocontrol;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.meter.api.SymmetricMeter;
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

	private ZonedDateTime lastsent = null;

	// inverter values for MeteoControl
	private long e_int = 0; // Energy generated per interval
	private int u_ac = 0; // Voltage AC
	private int p_ac = 0; // Power AC (single phase or accumulated)

	// meter values for MeteoControl
	private int m_ac_f = 0; // Grid frequency
	private int m_ac_q = 0; // Reactive Power
	private int m_ac_p = 0;

	// battery values for MeteoControl
	private int b_charge_level = 0; // SOC
	private int b_p_dc = 0; // Total battery power

	private Document xmlDoc = null;

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

		this.lastsent = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusMinutes(this.config.tInterval());

	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		ZonedDateTime now = ZonedDateTime.now();

		ZonedDateTime tmp = now.truncatedTo(ChronoUnit.MINUTES);

		if (tmp.minusMinutes(this.config.tInterval()).equals(this.lastsent)) {
			this.lastsent = tmp;
			this.collectData(tmp);
			String data = this.formatData();

			new Thread(() -> {
				try {
					this.sendData(data);
					this.logInfo(this.log, "Measurements succesfully transmitted to Meteo Control!");
				} catch (OpenemsException e) {
					// TODO Auto-generated catch block
					this.logError(this.log, e.getMessage());
				}
			}).start();
		}

	}

	private void collectData(ZonedDateTime toDate) throws OpenemsNamedException {

		Element datapoints = null;

		try {
			datapoints = this.createXMLBody();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new OpenemsException(e.getMessage());
		}

		SymmetricEss ess = this.componentManager.getComponent(this.config.essId());
		SymmetricMeter pvInverter = this.componentManager.getComponent(this.config.pvInverter());
		SymmetricMeter meter = this.componentManager.getComponent(this.config.meter());

		ChannelAddress e_intAddress = pvInverter.getActiveProductionEnergyChannel().address();
		ChannelAddress p_acAddress = pvInverter.getActivePowerChannel().address();
		ChannelAddress u_acAddress = pvInverter.getVoltageChannel().address();

		ChannelAddress m_ac_fAddress = meter.getFrequencyChannel().address();
		ChannelAddress m_ac_qAddress = meter.getReactivePowerChannel().address();
		ChannelAddress m_ac_pAddress = meter.getActivePowerChannel().address();

		ChannelAddress b_charge_levelAddress = ess.getSocChannel().address();
		ChannelAddress b_p_dcAddress = ess.getActivePowerChannel().address();

		Set<ChannelAddress> eChannels = new HashSet<>();

		eChannels.add(e_intAddress);

		Set<ChannelAddress> channels = new HashSet<>();

		channels.add(p_acAddress);
		channels.add(u_acAddress);

		channels.add(m_ac_fAddress);
		channels.add(m_ac_qAddress);
		channels.add(m_ac_pAddress);

		channels.add(b_charge_levelAddress);
		channels.add(b_p_dcAddress);

		ZonedDateTime fromDate = toDate.minusMinutes(this.config.tInterval());

		try {
			SortedMap<ChannelAddress, JsonElement> eHistory = this.getTimedata().queryHistoricEnergy(null, fromDate,
					toDate, eChannels);

			this.e_int = this.getHistoryDataValue(eHistory, e_intAddress);
		} catch (OpenemsNamedException e) {
			this.e_int = 0;

		}

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> history = this.getTimedata()
				.queryHistoricData(null, fromDate, toDate, channels, this.config.mInterval());
		for (ZonedDateTime t : history.keySet()) {

			SortedMap<ChannelAddress, JsonElement> tmp = history.get(t);
			String timestamp = t.truncatedTo(ChronoUnit.MINUTES).format(DateTimeFormatter.ISO_INSTANT).toString();

			// this.p_dc = this.getHistoryDataValue(tmp, p_dcAddress);
			this.u_ac = this.getHistoryDataValue(tmp, u_acAddress);
			this.p_ac = this.getHistoryDataValue(tmp, p_acAddress);

			this.m_ac_f = this.getHistoryDataValue(tmp, m_ac_fAddress);
			this.m_ac_q = this.getHistoryDataValue(tmp, m_ac_qAddress);
			this.m_ac_p = this.getHistoryDataValue(tmp, m_ac_pAddress);

			this.b_charge_level = this.getHistoryDataValue(tmp, b_charge_levelAddress);
			this.b_p_dc = this.getHistoryDataValue(tmp, b_p_dcAddress);

			Element datapoint = this.xmlDoc.createElement("datapoint");
			datapoint.setAttribute("interval", Integer.toString(this.config.mInterval()));
			datapoint.setAttribute("timestamp", timestamp);

			Element inverter = this.xmlDoc.createElement("device");
			inverter.setAttribute("id", "inverter-1");
			datapoint.appendChild(inverter);

			inverter.appendChild(this.appendMeasurement("E_INT", Long.toString(this.e_int)));
			inverter.appendChild(this.appendMeasurement("P_AC", Integer.toString(this.p_ac)));
			inverter.appendChild(this.appendMeasurement("U_AC", Integer.toString(this.u_ac)));

			Element meterElement = this.xmlDoc.createElement("device");
			meterElement.setAttribute("id", "meter-1");
			datapoint.appendChild(meterElement);

			meterElement.appendChild(this.appendMeasurement("M_AC_F", Integer.toString(this.m_ac_f)));
			meterElement.appendChild(this.appendMeasurement("M_AC_Q", Integer.toString(this.m_ac_q)));
			meterElement.appendChild(this.appendMeasurement("M_AC_P", Integer.toString(this.m_ac_p)));

			Element battery = this.xmlDoc.createElement("device");
			battery.setAttribute("id", "battery-1");
			datapoint.appendChild(battery);

			battery.appendChild(this.appendMeasurement("B_CHARGE_LEVEL", Integer.toString(this.b_charge_level)));
			battery.appendChild(this.appendMeasurement("B_P_DC", Integer.toString(this.b_p_dc)));

			datapoints.appendChild(datapoint);

		}

	}

	private String formatData() throws OpenemsException {

		// Transform Document to XML String
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer;
		try {
			transformer = tf.newTransformer();
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new OpenemsException(e.getMessageAndLocation());
		}
		StringWriter writer = new StringWriter();

		try {
			transformer.transform(new DOMSource(this.xmlDoc), new StreamResult(writer));
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			throw new OpenemsException(e.getMessageAndLocation());
		}

		return writer.getBuffer().toString();
	}

	private Timedata getTimedata() throws OpenemsException {
		if (this.timedata != null) {
			return this.timedata;
		}
		throw new OpenemsException("There is no Timedata-Service available!");
	}

	private void sendData(String data) throws OpenemsException {
		HttpPost httpPost = new HttpPost(this.config.host() + "/v2/?apiKey=" + this.config.apikey());

		httpPost.setHeader("Content-Type", "application/xml");
		try {
			httpPost.setEntity(new StringEntity(data));
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			throw new OpenemsException(e1.getMessage());

		}

		CloseableHttpClient httpclient = HttpClientBuilder.create().build();

		try {
			CloseableHttpResponse response = httpclient.execute(httpPost);

			if (response.getStatusLine().getStatusCode() != 202) {
				String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
				throw new OpenemsException(response.getStatusLine().toString() + System.lineSeparator() + responseBody);
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

	private Element createXMLBody() throws ParserConfigurationException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		this.xmlDoc = docBuilder.newDocument();

		Element mii = this.xmlDoc.createElement("mii");
		mii.setAttribute("version", "2.0");
		mii.setAttribute("targetNamespace", "http://api.sspcdn.com/mii");
		mii.setAttribute("xmlns", "http://api.sspcdn.com/mii");
		this.xmlDoc.appendChild(mii);

		Element datalogger = this.xmlDoc.createElement("datalogger");
		mii.appendChild(datalogger);

		Element configuration = this.xmlDoc.createElement("configuration");
		configuration.setAttribute("xmlns", "http://api.sspcdn.com/mii/datalogger/configuration");
		datalogger.appendChild(configuration);

		Element uuid = this.xmlDoc.createElement("uuid");
		configuration.appendChild(uuid);

		Element vendor = this.xmlDoc.createElement("vendor");
		vendor.setTextContent("KACO new energy GmbH");
		uuid.appendChild(vendor);

		Element serial = this.xmlDoc.createElement("serial");
		serial.setTextContent(this.config.serial());
		uuid.appendChild(serial);

		Element devices = this.xmlDoc.createElement("devices");
		configuration.appendChild(devices);

		Element inverter = this.xmlDoc.createElement("device");
		inverter.setAttribute("type", "inverter");
		inverter.setAttribute("id", "inverter-1");
		devices.appendChild(inverter);

		Element inverterUid = this.xmlDoc.createElement("uid");
		inverterUid.setTextContent(this.config.pvInverter());
		inverter.appendChild(inverterUid);

		Element meter = this.xmlDoc.createElement("device");
		meter.setAttribute("type", "meter");
		meter.setAttribute("id", "meter-1");
		devices.appendChild(meter);

		Element meterUid = this.xmlDoc.createElement("uid");
		meterUid.setTextContent(this.config.meter());
		meter.appendChild(meterUid);

		Element battery = this.xmlDoc.createElement("device");
		battery.setAttribute("type", "battery");
		battery.setAttribute("id", "battery-1");
		devices.appendChild(battery);

		Element batteryUid = this.xmlDoc.createElement("uid");
		batteryUid.setTextContent(this.config.essId());
		battery.appendChild(batteryUid);

		Element datapoints = this.xmlDoc.createElement("datapoints");
		datapoints.setAttribute("xmlns", "http://api.sspcdn.com/mii/datalogger/datapoints");
		datalogger.appendChild(datapoints);

		return datapoints;

	}

	private Element appendMeasurement(String name, String value) {

		Element mv = this.xmlDoc.createElement("mv");
		mv.setAttribute("t", name);
		mv.setAttribute("v", value);

		return mv;

	}

}
