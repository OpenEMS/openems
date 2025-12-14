package io.openems.edge.simulator.datasource.single.channel;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.simulator.DataContainer;
import io.openems.edge.simulator.datasource.api.AbstractDatasource;
import io.openems.edge.simulator.datasource.api.SimulatorDatasource;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.Datasource.Single.Channel", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE //
})
public class SimulatorDatasourceSingleChannelImpl extends AbstractDatasource
		implements SimulatorDatasourceSingleChannel, SimulatorDatasource, OpenemsComponent, EventHandler {

	@Reference
	private ComponentManager componentManager;

	private volatile Float currentValue = null;

	public SimulatorDatasourceSingleChannelImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SimulatorDatasourceSingleChannel.ChannelId.values() //
		);
		this.getDataWriteChannel().onSetNextWrite(newData -> {
			if (newData != null) {
				this.currentValue = newData.floatValue();
			}
		});
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws NumberFormatException, IOException {
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
		var container = new DataContainer();
		// Initialize with default value 0
		container.addRecord(new Float[] { 0f });
		return container;
	}

	@Override
	public <T> T getValue(OpenemsType type, ChannelAddress channelAddress) {
		var value = this.currentValue;
		if (value == null) {
			// Fall back to default value from container
			return super.getValue(type, channelAddress);
		}
		return TypeUtils.getAsType(type, value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> getValues(OpenemsType type, ChannelAddress channelAddress) {
		var value = this.currentValue;
		if (value == null) {
			// Fall back to default values from container
			return super.getValues(type, channelAddress);
		}
		T typedValue = (T) TypeUtils.getAsType(type, value);
		return List.of(typedValue);
	}

	@Override
	public Set<String> getKeys() {
		return Set.of();
	}
}
