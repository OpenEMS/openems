package io.openems.edge.timedata.influxdb;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

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

import com.zaxxer.influx4j.InfluxDB;
import com.zaxxer.influx4j.Point;
import com.zaxxer.influx4j.PointFactory;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.controllerexecutor.EdgeEventConstants;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Timedata.InfluxDB", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
public class Influxdb extends AbstractOpenemsComponent implements Timedata, OpenemsComponent, EventHandler {

	private final static String MEASUREMENT = "data";

	private final Logger log = LoggerFactory.getLogger(Influxdb.class);

	private InfluxDB influxDB = null;
	private PointFactory pointFactory = null;
	private int pointFactoryMaxSize = 0;

	private List<OpenemsComponent> _components = new CopyOnWriteArrayList<>();

	@Reference(policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.AT_LEAST_ONE, //
			target = "(&(enabled=true)(!(service.factoryPid=Timedata.InfluxDB)))")
	protected void addComponent(OpenemsComponent component) {
		this._components.add(component);
		this.updatePointFactory();
	}

	protected void removeComponent(OpenemsComponent component) {
		this._components.remove(component);
		this.updatePointFactory();
	}

	private synchronized void updatePointFactory() {
		int noOfChannels = 0;
		for (OpenemsComponent component : this._components) {
			noOfChannels += component.channels().size();
		}
		int calculatedMaxSize = Math.round(noOfChannels * 1.1f) + 1;
		if (this.pointFactory == null || this.pointFactoryMaxSize < calculatedMaxSize
				|| this.pointFactoryMaxSize > calculatedMaxSize * 1.1f) {
			// create a new PointFactory
			this.pointFactory = PointFactory.builder() //
					.initialSize(calculatedMaxSize) //
					.maximumSize(calculatedMaxSize) //
					.build();
			this.pointFactoryMaxSize = calculatedMaxSize;
		}
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		this.updatePointFactory();
		this.influxDB = InfluxDB.builder() //
				.setConnection(config.ip(), config.port(), InfluxDB.Protocol.HTTP) //
				.setUsername(config.username()) //
				.setPassword(config.password()) //
				.setDatabase(config.database()) //
				.build();
		influxDB.createDatabase("db");
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.collectChannelValues();
			break;
		}
	}

	private synchronized void collectChannelValues() {
		long timestamp = System.currentTimeMillis() / 1000;
		final Point point = this.pointFactory //
				.createPoint(MEASUREMENT) //
				.timestamp(timestamp, TimeUnit.SECONDS);
		this._components.stream().filter(c -> c.isEnabled()).forEach(component -> {
			component.channels().forEach(channel -> {
				Optional<?> valueOpt = channel.getActiveValueOpt();
				if (!valueOpt.isPresent()) {
					// ignore not available channels
					return;
				}
				Object value = valueOpt.get();
				String address = channel.address().toString();
				try {
					switch (channel.getType()) {
					case BOOLEAN:
						point.field(address, (Boolean) value);
						break;
					case FLOAT:
						point.field(address, (Float) value);
						break;
					case INTEGER:
						point.field(address, (Integer) value);
						break;
					case LONG:
						point.field(address, (Long) value);
						break;
					case SHORT:
						point.field(address, (Short) value);
						break;
					}
				} catch (IllegalArgumentException e) {
					this.logWarn(this.log,
							"Unable to add Channel [" + address + "] value [" + value + "]: " + e.getMessage());
				}
			});
		});
		this.influxDB.write(point);
	}
}
