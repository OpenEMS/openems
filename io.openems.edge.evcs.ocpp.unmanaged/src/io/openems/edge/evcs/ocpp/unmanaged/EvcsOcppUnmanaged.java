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

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.OcppEvcs;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Evcs.Ocpp.Unmanaged", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class EvcsOcppUnmanaged extends AbstractOpenemsComponent implements Evcs, OcppEvcs, OpenemsComponent, EventHandler {

	//private final WriteHandler writeHandler = new WriteHandler(this);

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId{
		;
		private Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}
		@Override
		public Doc doc() {
			return this.doc;
		}
	}
	
	public EvcsOcppUnmanaged() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				OcppEvcs.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		
		this.channel(OcppEvcs.ChannelId.OCPP_ID).setNextValue(config.ocpp_id());
		this.channel(OcppEvcs.ChannelId.CONNECTOR_ID).setNextValue(config.chargerId());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:

			// handle writes
			//this.writeHandler.run();
			break;
		}
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}
}
