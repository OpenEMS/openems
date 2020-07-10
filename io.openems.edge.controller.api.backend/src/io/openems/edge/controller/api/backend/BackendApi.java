package io.openems.edge.controller.api.backend;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.notification.EdgeConfigNotification;
import io.openems.common.jsonrpc.notification.SystemLogNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.OpenemsType;
import io.openems.common.websocket.AbstractWebsocketClient;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.common.ApiWorker;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Api.Backend", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"org.ops4j.pax.logging.appender.name=Controller.Api.Backend", //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CONFIG_UPDATE //
		} //
)
public class BackendApi extends AbstractOpenemsComponent
		implements Controller, OpenemsComponent, PaxAppender, EventHandler {

	protected static final int DEFAULT_NO_OF_CYCLES = 10;
	protected static final String COMPONENT_NAME = "Controller.Api.Backend";

	protected final BackendWorker worker = new BackendWorker(this);

	protected final ApiWorker apiWorker = new ApiWorker();

	private final Logger log = LoggerFactory.getLogger(BackendApi.class);

	protected WebsocketClient websocket = null;
	protected int noOfCycles = DEFAULT_NO_OF_CYCLES; // default, is going to be overwritten by config
	protected boolean debug = false;

	// Used for SubscribeSystemLogRequests
	private boolean isSystemLogSubscribed = false;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	protected ComponentManager componentManager;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SUCCESSFULLY_SENT(Doc.of(OpenemsType.BOOLEAN) //
				.text(" sending to Backend was successful ")), //
		LAST_RESENT_DATA(Doc.of(OpenemsType.LONG) //
				.text("last timestamp data sent to backend")),
		BACKEND_CONNECTED(Doc.of(OpenemsType.BOOLEAN) //
				.text("Connected to the backend."));
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public BackendApi() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.noOfCycles = config.noOfCycles();
		this.debug = config.debug();

		if (!this.isEnabled()) {
			return;
		}

		// initialize ApiWorker
		this.apiWorker.setTimeoutSeconds(config.apiTimeout());

		// Get URI
		URI uri = null;
		try {
			uri = new URI(config.uri());
		} catch (URISyntaxException e) {
			log.error("URI [" + config.uri() + "] is invalid: " + e.getMessage());
			return;
		}

		// Get Proxy configuration
		Proxy proxy;
		if (config.proxyAddress().trim().equals("") || config.proxyPort() == 0) {
			proxy = AbstractWebsocketClient.NO_PROXY;
		} else {
			proxy = new Proxy(config.proxyType(), new InetSocketAddress(config.proxyAddress(), config.proxyPort()));
		}

		// create http headers
		Map<String, String> httpHeaders = new HashMap<>();
		httpHeaders.put("apikey", config.apikey());

		// Create Websocket instance
		this.websocket = new WebsocketClient(this, COMPONENT_NAME + ":" + this.id(), uri, httpHeaders, proxy);
		this.websocket.start();

		// Activate worker
		this.worker.activate(config.id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.worker.deactivate();
		if (this.websocket != null) {
			this.websocket.stop();
		}
	}

	@Override
	public void run() throws OpenemsNamedException {

		BooleanReadChannel connectionStatus = this.channel(ChannelId.BACKEND_CONNECTED);

		log.info("Backend Connected Channel: " + connectionStatus.getNextValue().get());

		if (connectionStatus.getNextValue().get() != null) {
			if (connectionStatus.getNextValue().get() == true) {
				this.resendHistoricData();
			}
		}

		this.apiWorker.run();
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logInfo(log, message);
	}

	private void resendHistoricData() throws OpenemsNamedException {

		ZonedDateTime fromDate;
		ZonedDateTime toDate = ZonedDateTime.now();

		LongReadChannel lastResentData = this.channel(ChannelId.LAST_RESENT_DATA);

		if (lastResentData.value().get() == null) {
			fromDate = ZonedDateTime.of(2020, 07, 01, 00, 00, 00, 00, ZoneId.of("UTC"));
		} else {
			Instant istant = Instant.ofEpochMilli(lastResentData.value().get());
			fromDate = ZonedDateTime.ofInstant(istant, ZoneId.of("UTC"));
		}

		// Querying the historic data.
		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queriedhistoricData = new TreeMap<>();
		queriedhistoricData = this.queryHistoricData(fromDate, toDate);

		// Pre-process and send the queried data
		this.resendData(queriedhistoricData, lastResentData, fromDate, toDate);

	}

	private SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(ZonedDateTime fromDate,
			ZonedDateTime toDate) throws OpenemsNamedException {

		TreeSet<ChannelAddress> addresses = new TreeSet<>();

		this.componentManager.getEnabledComponents().parallelStream() //
				.filter(c -> c.isEnabled()) //
				.flatMap(component -> component.channels().parallelStream()) //
				.filter(channel -> // Ignore WRITE_ONLY Channels
				channel.channelDoc().getAccessMode() == AccessMode.READ_ONLY
						|| channel.channelDoc().getAccessMode() == AccessMode.READ_WRITE) //
				.filter(name -> name.address().toString().contains("_sum")) //
				.filter(name -> (name.address().toString().contains("EssSoc")
						|| name.address().toString().contains("EssActivePower")
						|| name.address().toString().contains("ProductionActivePower")
						|| name.address().toString().contains("ConsumptionActivePower")
						|| name.address().toString().contains("GridActivePower")))//
//				.filter(name -> !name.address().toString().contains("EssActivePowerL")
//						&& !name.address().toString().contains("GridActivePowerL")
//						&& !name.address().toString().contains("ConsumptionActivePowerL")) //

				.forEach(channel -> {
					addresses.add(channel.address());
//					System.out.println("addresses : " + channel.address());
				});

		// Querying the historic data.
		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queriedHistoricdata = new TreeMap<>();

		// Currently checking average of 60 seconds.
		queriedHistoricdata = this.timedata.queryHistoricData(null, fromDate, toDate, addresses, 10);

		return queriedHistoricdata;

	}

	private void resendData(SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queriedhistoricData,
			LongReadChannel lastResentData, ZonedDateTime fromDate, ZonedDateTime toDate) {
		// Prepare message values and create JSON-RPC notification
		TimestampedDataNotification message = new TimestampedDataNotification();

		// Check if its first time running.
		if (lastResentData.value().get() == null) {
			queriedhistoricData.entrySet().forEach(entry -> {
				// Check for null and send only non null values.
				SortedMap<ChannelAddress, JsonElement> values = new TreeMap<>();
//				log.info(" checkEntry: " + "Key : " + entry.getKey() + " Value : " + entry.getValue());
				entry.getValue().entrySet().forEach(checkEntry -> {
					if (!checkEntry.getValue().isJsonNull()) {
						values.put(checkEntry.getKey(), checkEntry.getValue());
						long timestamp = entry.getKey().toLocalDateTime().toInstant(ZoneOffset.UTC).toEpochMilli();
						message.add(timestamp, values);
					}
				});
			});
		} else {

			// extract the values from queried data only during the time when the
			// successfully sent is false
			BooleanReadChannel successfullySentChannel = this.channel(ChannelId.SUCCESSFULLY_SENT);

			TreeMap<ZonedDateTime, Boolean> successfullySentValues = new TreeMap<>();

			successfullySentChannel.getPastValues().subMap(fromDate.toLocalDateTime(), toDate.toLocalDateTime())
					.entrySet().forEach(entry -> {
//				log.info("entry Value: " + entry.getValue().get());
						if (entry.getValue().get() != null) {
							if (entry.getValue().get() == false) {

								// Converting LocalDateTime to ZonedDateTime
								LocalDateTime key = entry.getKey();
								ZoneId id = ZoneId.of("Europe/Berlin");

								successfullySentValues.put(key.atZone(id), entry.getValue().get());
//							System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
							}
						}
					});

			queriedhistoricData.subMap(successfullySentValues.firstKey(), successfullySentValues.lastKey()).entrySet()
					.forEach(entry -> {

						// Check for null and send only non null values.
						SortedMap<ChannelAddress, JsonElement> values = entry.getValue();

						entry.getValue().entrySet().forEach(checkEntry -> {
							if (!checkEntry.getValue().isJsonNull()) {
								values.put(checkEntry.getKey(), checkEntry.getValue());
							}
						});

//					System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
						long timestamp = entry.getKey().toLocalDateTime().toInstant(ZoneOffset.UTC).toEpochMilli();
						message.add(timestamp, values);

					});
		}

		// Send the JsonRPC request
		if (!message.getData().isEmpty()) {
			log.info("message is not empty_______________________________");
//			log.info("Message is ==================================> " + message);
			this.websocket.sendMessage(message);
			log.info("Sent_________________________________________________");
			this.channel(ChannelId.LAST_RESENT_DATA).setNextValue(Instant.now().toEpochMilli());
		}
	}

	/**
	 * Activates/deactivates subscription to System-Log.
	 * 
	 * <p>
	 * If activated, all System-Log events are sent via
	 * {@link SystemLogNotification}s.
	 * 
	 * @param isSystemLogSubscribed true to activate
	 */
	protected void setSystemLogSubscribed(boolean isSystemLogSubscribed) {
		this.isSystemLogSubscribed = isSystemLogSubscribed;
	}

	@Override
	public void doAppend(PaxLoggingEvent event) {
		if (!this.isSystemLogSubscribed) {
			return;
		}
		WebsocketClient ws = this.websocket;
		if (ws == null) {
			return;
		}
		SystemLogNotification notification = SystemLogNotification.fromPaxLoggingEvent(event);
		ws.sendMessage(notification);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.worker.triggerNextRun();
			break;

		case EdgeEventConstants.TOPIC_CONFIG_UPDATE:
			EdgeConfig config = (EdgeConfig) event.getProperty(EdgeEventConstants.TOPIC_CONFIG_UPDATE_KEY);
			EdgeConfigNotification message = new EdgeConfigNotification(config);
			WebsocketClient ws = this.websocket;
			if (ws == null) {
				return;
			}
			ws.sendMessage(message);
		}
	}
}
