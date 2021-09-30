package io.openems.edge.shelly.core;

import java.util.Iterator;
import java.util.List;

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
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.io.api.DigitalOutput;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "io.openems.edge.shelly.core", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		} //
)
public class ShellyCoreImpl extends AbstractOpenemsComponent implements ShellyCore, OpenemsComponent, EventHandler {

	private Config config = null;
	private ShellyApi api = null;
	private List<SymmetricMeter> meters;
	private List<DigitalOutput> outputs;

	public ShellyCoreImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ShellyCore.ChannelId.values() //				
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.api = new ShellyApi(this,config.ip());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:			
			api.iterate();			
			_setCommunicationFailed(api.getCommFailed());
			if(!api.getCommFailed()) {
				updateMeters();				
				updateIos();
			}
			break;
		}
	}

	@Override
	public String debugLog() {
		//return "L:" + this.getActivePower().asString();
		if(api != null) {
			return "Shelly: "+ api.getType();
		}
		return "Shelly: not valid"; 
		
	}
	
	void registerMeter(SymmetricMeter m) {
		if(m != null) {
			this.meters.add(m);
		}
	}
	
	void registerIo(DigitalOutput o) {
		if(o!= null) {
			this.outputs.add(o);
		}
	}	
	
	protected void updateMeters() {
		for (Iterator<SymmetricMeter> it = meters.iterator(); it.hasNext();) {
			SymmetricMeter meter = it.next();   
			meter._setActivePower(api.getActivePower());
		}
	}
	
	protected void updateIos() {
		
	}
}
