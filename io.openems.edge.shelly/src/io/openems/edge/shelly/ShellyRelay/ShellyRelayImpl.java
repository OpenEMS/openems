package io.openems.edge.shelly.ShellyRelay;

import java.util.Optional;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import com.google.gson.JsonObject;

import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.shelly.core.ShellyComponent;
import io.openems.edge.shelly.core.ShellyCore;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Shelly.ShellyRelay", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE //
		)
public class ShellyRelayImpl extends AbstractOpenemsComponent
		implements ShellyRelay, DigitalOutput, ShellyComponent, OpenemsComponent  {
	
	private final BooleanWriteChannel[] digitalOutputChannels;
	private int relayIndex=0;
	
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected ShellyCore core;
	
	@Reference
	protected ConfigurationAdmin cm;
	
	public ShellyRelayImpl() {
		super(OpenemsComponent.ChannelId.values(), // 
				ShellyComponent.ChannelId.values(), //						
				DigitalOutput.ChannelId.values(), //
				ShellyRelay.ChannelId.values() //
				);
		
		this.digitalOutputChannels = new BooleanWriteChannel[] { //
				this.channel(ShellyRelay.ChannelId.RELAY) //
		};
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.relayIndex = config.relay_index();
		//this.meterType = config.type();
		// update filter for 'core'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "core", config.core_id())) {
			return;
		}
		// Register with core...
		this.core.registerClient(this);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		// Unregister with core....
		core.unregisterClient(this);
	}
	
	@Override
	public Boolean wantsExtendedData() {
		// No
		return false;
	}

	@Override
	public Integer wantedIndex() {
		// We need the index as specified in the config
		return relayIndex;
	}

	@Override
	public Boolean setBaseChannels() {
		// Yes, please
		return true;
	}
	
	@Override
	public void setExtendedData(JsonObject json) {
		// Nothing to do here.
	}

	
	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {		
		return this.digitalOutputChannels;
	}
	
		
	@Override
	public String debugLog() {
		StringBuilder b = new StringBuilder("R: ");
		Optional<Boolean> valueOpt = this.getRelayChannel().value().asOptional();
		if (valueOpt.isPresent()) {
			b.append(valueOpt.get() ? "On" : "Off");
		} else {
			b.append("Unknown");
		}		
		b.append("|");		
		return b.toString();
	}
}
