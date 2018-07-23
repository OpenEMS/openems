package io.openems.edge.simulator.datasource.csv;

import java.util.Set;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.simulator.datasource.api.SimulatorDatasource;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Simulator.Datasource.CSVReader", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE)
public class CSVDatasource extends AbstractOpenemsComponent
		implements SimulatorDatasource, EventHandler {

	private DataContainer data;

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled());

		// read csv-data
		this.data = Util.getValues(config.source(), config.multiplier());
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE:
			this.data.nextRecord();
			break;
		}
	}

	@Override
	public <T> T getValue(OpenemsType type, String key) {
		return TypeUtils.getAsType(type, this.data.getValue(key));
	}

	@Override
	public Set<String> getKeys() {
		return this.data.getKeys();
	}

	@Override
	public int getTimeDelta() {
		return 15 /* minutes */ * 60;
	}

}
