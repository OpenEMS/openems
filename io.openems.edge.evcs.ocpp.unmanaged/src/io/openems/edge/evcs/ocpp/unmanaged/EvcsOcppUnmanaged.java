package io.openems.edge.evcs.ocpp.unmanaged;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.Evcs;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Evcs.Ocpp.Unmanaged", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class EvcsOcppUnmanaged extends AbstractOpenemsComponent implements Evcs, OpenemsComponent, EventHandler {

	private Boolean lastConnectionLostState = false;
	//private final WriteHandler writeHandler = new WriteHandler(this);

	public EvcsOcppUnmanaged() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				EvcsOcppUnmanagedChannelId.values() //

		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:

			// Clear channels if the connection to the Charging Station has been lost
			Channel<Boolean> connectionLostChannel = this.channel(EvcsOcppUnmanagedChannelId.CHARGINGSTATION_COMMUNICATION_FAILED);
			Boolean connectionLost = connectionLostChannel.value().orElse(lastConnectionLostState);
			if (connectionLost != lastConnectionLostState) {
				if (connectionLost) {
					resetChannelValues();
				}
				lastConnectionLostState = connectionLost;
			}

			// handle writes
			//this.writeHandler.run();
			break;
		}
	}

	/**
	 * Resets all channel values except the Communication_Failed channel
	 */
	private void resetChannelValues() {
		for (EvcsOcppUnmanagedChannelId c : EvcsOcppUnmanagedChannelId.values()) {
			if (c != EvcsOcppUnmanagedChannelId.CHARGINGSTATION_COMMUNICATION_FAILED) {
				Channel<?> channel = this.channel(c);
				channel.setNextValue(null);
			}
		}
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}
}
