package io.openems.edge.shelly.Shelly3EM;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;
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
		implements   AsymmetricMeter, SymmetricMeter, ShellyComponent, OpenemsComponent  {

	private final Logger log = LoggerFactory.getLogger(Shelly3EmImpl.class);
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
				DigitalOutput.ChannelId.values() //
				);
				
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.meterType = config.type();
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
		// Currently not
		// In fact we would in order to sum all legs for the symmetric meter.
		return true;
	}

	@Override
	public Integer wantedIndex() {
		// We need index 0
		return 0;
	}

	@Override
	public Boolean setBaseChannels() {
		return false;
	}
	
	@Override
	public void setExtendedData(JsonObject json) {
		Integer totalPower = null;
		
		Integer[] powers = new Integer[3];
		Integer[] currents = new Integer[3];
		Integer[] voltages = new Integer[3];
		Double[] powerfactors = new Double[3];
		
		Long total = 0L;
		Long totalReturned = 0L;
		try {			
			totalPower =  TypeUtils.getAsType(OpenemsType.INTEGER, JsonUtils.getAsDouble(json,"total_power"));			
			/**
			 * Read the three emeters
			 */			
			JsonArray emeters = JsonUtils.getAsJsonArray(json, "emeters");
			JsonObject meter;
			for(int i=0;i<3;++i) {
				meter = JsonUtils.getAsJsonObject(emeters.get(i));
				Double rawValue;
				
				// Why is power in W while current and voltage are in mX???
				powers[i] =  TypeUtils.getAsType(OpenemsType.INTEGER,JsonUtils.getAsDouble(meter,"power"));				
				powerfactors[i] = JsonUtils.getAsDouble(meter,"pf");
				
				rawValue = JsonUtils.getAsDouble(meter,"current");
				rawValue = TypeUtils.multiply(rawValue,1000.0);
				currents[i] =  TypeUtils.getAsType(OpenemsType.INTEGER,rawValue);
				
				rawValue = JsonUtils.getAsDouble(meter,"voltage");
				rawValue = TypeUtils.multiply(rawValue,1000.0);				
				voltages[i] = TypeUtils.getAsType(OpenemsType.INTEGER,rawValue);
					
				total  = TypeUtils.sum(total,TypeUtils.getAsType(OpenemsType.LONG,JsonUtils.getAsDouble(meter,"total")));
				totalReturned  = TypeUtils.sum(totalReturned,TypeUtils.getAsType(OpenemsType.LONG,JsonUtils.getAsDouble(meter,"total_returned")));
			}
		} catch (OpenemsNamedException | IndexOutOfBoundsException e) {
			this.logError(this.log, "Unable to read from Shelly API: " + e.getMessage());
			this._setSlaveCommunicationFailed(true);
		} finally {		
			this._setVoltage(TypeUtils.averageRounded(voltages[0],voltages[1],voltages[2]));
			this._setVoltageL1(voltages[0]);
			this._setVoltageL2(voltages[1]);
			this._setVoltageL3(voltages[2]);
			
			// Does this has to be negative?
			this._setActiveProductionEnergy(totalReturned);
			this._setActiveConsumptionEnergy(total);
			
			switch (this.meterType) {
			case GRID:
			case PRODUCTION_AND_CONSUMPTION:
			case PRODUCTION:
				this._setActivePower(totalPower);
				this._setActivePowerL1(powers[0]);
				this._setActivePowerL2(powers[1]);
				this._setActivePowerL3(powers[2]);
				
				this._setCurrent(TypeUtils.sum(currents[0],currents[1],currents[2]));
				this._setCurrentL1(currents[0]);
				this._setCurrentL2(currents[1]);
				this._setCurrentL3(currents[2]);
				break;
			case CONSUMPTION_NOT_METERED: // to be validated
			case CONSUMPTION_METERED: // to be validated			
				this._setActivePower(TypeUtils.multiply(totalPower, -1)); // invert
				this._setActivePowerL1(TypeUtils.multiply(powers[0], -1)); // invert
				this._setActivePowerL2(TypeUtils.multiply(powers[1], -1)); // invert
				this._setActivePowerL3(TypeUtils.multiply(powers[2], -1)); // invert
				
				this._setCurrent(TypeUtils.multiply(TypeUtils.sum(currents[0],currents[1],currents[2]),-1));
				this._setCurrentL1(TypeUtils.multiply(currents[0], -1));
				this._setCurrentL2(TypeUtils.multiply(currents[1], -1));
				this._setCurrentL3(TypeUtils.multiply(currents[2], -1));				
				break;
			}						
		}				
	}

	@Override
	public MeterType getMeterType() {		
		return this.meterType;
	}

		
	@Override
	public String debugLog() {
		StringBuilder b = new StringBuilder();		
		b.append("L: "+ this.getActivePower().asString());
		b.append(" L1: "+ this.getActivePowerL1().asString());
		b.append(" L2: "+ this.getActivePowerL2().asString());
		b.append(" L3: "+ this.getActivePowerL3().asString());		
		b.append("|");		
		return b.toString();
	}
}
