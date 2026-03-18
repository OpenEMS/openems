package io.openems.edge.simulator.powercontrolunit;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.powercontrolunit.api.PowerControlUnit;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.PowerControlUnit", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class SimulatorPowerControlUnitImpl extends AbstractOpenemsComponent
		implements SimulatorPowerControlUnit, PowerControlUnit, EventHandler, OpenemsComponent {

	private Config config;

	public SimulatorPowerControlUnitImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SimulatorPowerControlUnit.ChannelId.values(), //
				PowerControlUnit.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
        if (EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE.equalsIgnoreCase(event.getTopic())) {
            this.updateChannels();
        }
	}

	private void updateChannels() {
		this._setMaxBuyFromGridLimit(this.config.buyFromGridLimit());
		this._setMaxSellToGridLimit(this.config.sellToGridLimit());
	}
}