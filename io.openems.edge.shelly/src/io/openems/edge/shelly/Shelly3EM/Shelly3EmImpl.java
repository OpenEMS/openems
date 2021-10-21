package io.openems.edge.shelly.Shelly3EM;

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
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.shelly.core.ShellyComponent;
import io.openems.edge.shelly.core.ShellyCore;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Shelly.Shelly3EM", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //				
				"type=GRID" //
		})
public class Shelly3EmImpl extends AbstractOpenemsComponent
		implements Shelly3EM, DigitalOutput, AsymmetricMeter, SymmetricMeter, ShellyComponent, OpenemsComponent  {

	private final Logger log = LoggerFactory.getLogger(Shelly3EmImpl.class);
	private final BooleanWriteChannel[] digitalOutputChannels;
	private MeterType meterType = MeterType.GRID;
	
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected ShellyCore core;
	
	@Reference
	protected ConfigurationAdmin cm;
	
	public Shelly3EmImpl() {
		super(OpenemsComponent.ChannelId.values(), // 
				ShellyComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //				
				DigitalOutput.ChannelId.values(), //
				Shelly3EM.ChannelId.values() //
				);
		
		this.digitalOutputChannels = new BooleanWriteChannel[] { //
				this.channel(Shelly3EM.ChannelId.RELAY) //
		};
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.meterType = config.type();
		// update filter for 'core'
		if (!OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "core", config.core_id())) {
			return;
		}
		// Register with core...
		core.registerClient(this);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		// Unregister with core....
		core.unregisterClient(this);
	}
	
	@Override
	public Boolean wantsExtendedData() {
		// Currently not
		// In fact we would in order to sum all legs for the symmetric meter.
		return false;
	}

	@Override
	public Integer wantedIndex() {
		// We need index 0
		return 0;
	}

	@Override
	public Boolean setBaseChannels() {
		return true;
	}
	
	@Override
	public void setExtendedData(JsonObject o) {
		// Currently not needed
	}

	@Override
	public MeterType getMeterType() {		
		return this.meterType;
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {		
		return this.digitalOutputChannels;
	}
	
	@Override
	public String debugLog() {
		StringBuilder b = new StringBuilder();
		Optional<Boolean> valueOpt = this.getRelayChannel().value().asOptional();
		if (valueOpt.isPresent()) {
			b.append(valueOpt.get() ? "On" : "Off");
		} else {
			b.append("Unknown");
		}
		b.append("|");
		b.append(this.getActivePowerChannel().value().asString());
		return b.toString();
	}
}
