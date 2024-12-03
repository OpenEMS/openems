package io.openems.edge.simulator.datasource.single.channel;

import java.io.IOException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.simulator.DataContainer;
import io.openems.edge.simulator.datasource.api.AbstractCsvDatasource;
import io.openems.edge.simulator.datasource.api.SimulatorDatasource;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.Datasource.Single.Channel", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
		}
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE, //
})
public class SimulatorDatasourceSingleChannelImpl extends AbstractCsvDatasource
		implements SimulatorDatasourceSingleChannel, SimulatorDatasource, OpenemsComponent, EventHandler {

	@Reference
	private ComponentManager componentManager;
	
	private DataContainer container = new DataContainer();
	
	private boolean containerIsEmpty = true;

	public SimulatorDatasourceSingleChannelImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SimulatorDatasourceSingleChannel.ChannelId.values() //
		);
		this.getDataWriteChannel().onSetNextWrite(newData -> {
			this.container.addRecord(new Float[] { newData.floatValue() });
			this.container.nextRecord();
		});
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws NumberFormatException, IOException {
		super.activate(context, config.id(), config.alias(), config.enabled(), -1);
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
		if (this.containerIsEmpty) {
			// fill with default value 0 if container is empty and no value set via channel yet
			this.container.addRecord(new Float[] { 0f });
			this.containerIsEmpty = false;
		}
		return this.container;
	}

}
