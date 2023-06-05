package io.openems.edge.simulator.thermometer;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.thermometer.api.Thermometer;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.Thermometer", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class SimulatorThermometerImpl extends AbstractOpenemsComponent
		implements SimulatorThermometer, Thermometer, OpenemsComponent {

	public SimulatorThermometerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Thermometer.ChannelId.values(), //
				SimulatorThermometer.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this._setTemperature(config.temperature());
	}

	@Override
	public String debugLog() {
		return this.getTemperature().toString();
	}

}
