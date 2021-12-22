package io.openems.edge.shelly.ShellyMeter;

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

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.shelly.core.ShellyComponent;
import io.openems.edge.shelly.core.ShellyCore;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Shelly.ShellyMeter", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //				
				"type=PRODUCTION" //
		})
public class ShellyMeterImpl extends AbstractOpenemsComponent
		implements SymmetricMeter, ShellyComponent, OpenemsComponent  {
	
	private MeterType meterType = MeterType.GRID;
	private int meterIndex=0;
	
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected ShellyCore core;
	
	@Reference
	protected ConfigurationAdmin cm;
	
	public ShellyMeterImpl() {
		super(OpenemsComponent.ChannelId.values(), // 
				ShellyComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values() //
				);	
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.meterType = config.type();
		this.meterIndex = config.meter_index();
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
		this.core.unregisterClient(this);
	}
	
	@Override
	public Boolean wantsExtendedData() {
		// Currently not
		// In fact we would in order to sum all legs for the symmetric meter.
		return false;
	}

	@Override
	public Integer wantedIndex() {
		// We need the index from the config
		return this.meterIndex;
	}

	@Override
	public Boolean setBaseChannels() {
		return true;
	}
	
	@Override
	public void setExtendedData(JsonObject json) {
		
	}

	@Override
	public MeterType getMeterType() {		
		return this.meterType;
	}
			
	@Override
	public String debugLog() {	
		return "L: "+ this.getActivePower().asString()+"|";
	}
}
