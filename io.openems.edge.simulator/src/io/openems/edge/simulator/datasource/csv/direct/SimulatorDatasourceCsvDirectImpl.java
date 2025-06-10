package io.openems.edge.simulator.datasource.csv.direct;

import java.io.IOException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.simulator.CsvUtils;
import io.openems.edge.simulator.DataContainer;
import io.openems.edge.simulator.datasource.api.AbstractCsvDatasource;
import io.openems.edge.simulator.datasource.api.SimulatorDatasource;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.Datasource.CSV.Direct", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE //
})
public class SimulatorDatasourceCsvDirectImpl extends AbstractCsvDatasource
		implements SimulatorDatasourceCsvDirect, SimulatorDatasource, OpenemsComponent, EventHandler {

	@Reference
	private ComponentManager componentManager;

	private Config config;

	public SimulatorDatasourceCsvDirectImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SimulatorDatasourceCsvDirect.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws NumberFormatException, IOException {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled(), config.timeDelta());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ComponentManager getComponentManager() {
		return this.componentManager;
	}

	@Override
	protected DataContainer getData() throws NumberFormatException, IOException {
		return CsvUtils.parseCsv(this.config.source(), this.config.format(), this.config.factor());
	}

}
