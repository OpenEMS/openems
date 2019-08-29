package io.openems.edge.simulator.datasource.csv;

import java.io.IOException;
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
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.simulator.CsvUtils;
import io.openems.edge.simulator.DataContainer;
import io.openems.edge.simulator.datasource.api.SimulatorDatasource;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Simulator.Datasource.CSVReader", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE)
public class CsvDatasource extends AbstractOpenemsComponent
		implements SimulatorDatasource, OpenemsComponent, EventHandler {

	private DataContainer data;

	private int timeDelta = 1;

	private boolean realtime = false;

	private long lastIteration;

	public CsvDatasource() {
		super(//
				OpenemsComponent.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws NumberFormatException, IOException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.timeDelta = config.timeDelta();
		this.realtime = config.realtime();
		this.lastIteration = System.currentTimeMillis();
		// read csv-data
		this.data = CsvUtils.readCsvFileFromRessource(CsvDatasource.class, config.source().filename, config.format(),
				config.factor());
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE:
			// don't change record, if realtime is set to true and timeDelta has not passed
			// yet
			if (realtime && System.currentTimeMillis() < lastIteration + 1000 /* second to millisecond */ * timeDelta) {
				return;
			}
			lastIteration = System.currentTimeMillis();
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
		return timeDelta;
	}

}
