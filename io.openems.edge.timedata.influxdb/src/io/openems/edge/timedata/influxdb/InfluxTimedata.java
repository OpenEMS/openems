package io.openems.edge.timedata.influxdb;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.controllerexecutor.EdgeEventConstants;
import io.openems.edge.timedata.api.Timedata;

/**
 * Provides read and write access to InfluxDB.
 * 
 * Two different libraries are used to access InfluxDB. The access is coupled in
 * the classes InfluxRead and InfluxWrite. {@link InfluxRead} is based on the
 * official org.influxdb.InfluxDB library. {@link InfluxWrite} is based on the
 * more efficient com.zaxxer.influx4j.InfluxDB library.
 * 
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Timedata.InfluxDB", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
public class InfluxTimedata extends AbstractOpenemsComponent implements Timedata, OpenemsComponent, EventHandler {

	protected final static String MEASUREMENT = "data";

	private final Logger log = LoggerFactory.getLogger(InfluxTimedata.class);

	private final InfluxRead influxRead;
	private final InfluxWrite influxWrite;

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/**
		 * FAULT: Unable to connect to InfluxDB
		 */
		STATE_0(new Doc().level(Level.FAULT).text("Unable to connect to InfluxDB (write)")), // refers to _influxDB1
		STATE_1(new Doc().level(Level.FAULT).text("Unable to connect to InfluxDB (read)")); // refers to _influxDB2

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	protected List<OpenemsComponent> _components = new CopyOnWriteArrayList<>();
	protected String ip;
	protected int port;
	protected String username;
	protected String password;
	protected String database;

	public InfluxTimedata() {
		this.influxRead = new InfluxRead(this);
		this.influxWrite = new InfluxWrite(this);
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.AT_LEAST_ONE, //
			target = "(&(enabled=true)(!(service.factoryPid=Timedata.InfluxDB)))")
	protected void addComponent(OpenemsComponent component) {
		this._components.add(component);
		this.influxWrite.updatePointFactory();
	}

	protected void removeComponent(OpenemsComponent component) {
		this._components.remove(component);
		this.influxWrite.updatePointFactory();
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		this.ip = config.ip();
		this.port = config.port();
		this.username = config.username();
		this.password = config.password();
		this.database = config.database();
		this.influxWrite.updatePointFactory();
		if (config.enabled()) {
			this.influxWrite.getConnection();
			try {
				this.influxRead.getConnection();
			} catch (OpenemsException e) {
				logWarn(this.log, e.getMessage());
			}
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.influxWrite.collectAndWriteChannelValues();
			break;
		}
	}

	@Override
	public JsonArray queryHistoricData(ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels,
			int resolution) throws OpenemsException {
		return this.influxRead.queryHistoricData(fromDate, toDate, channels, resolution);
	}
}
