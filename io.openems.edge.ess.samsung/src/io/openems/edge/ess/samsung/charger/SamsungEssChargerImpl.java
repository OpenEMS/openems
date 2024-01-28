package io.openems.edge.ess.samsung.charger;


import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.ess.samsung.common.SamsungApi;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "PV-Inverter.Samsung", immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE
)

@EventTopics({ //
	EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class SamsungEssChargerImpl extends AbstractOpenemsComponent
		implements SamsungEssCharger, ElectricityMeter, OpenemsComponent, EventHandler, TimedataProvider, ManagedSymmetricPvInverter{

	
	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;
	
	private final CalculateEnergyFromPower calculateActualEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	
	private final Logger log = LoggerFactory.getLogger(SamsungEssChargerImpl.class);
	private SamsungApi samsungApi = null;
	private MeterType meterType = null;



	public SamsungEssChargerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(),
				ManagedSymmetricPvInverter.ChannelId.values()//
				//
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
	    super.activate(context, config.id(), config.alias(), config.enabled());
	    this.samsungApi = new SamsungApi(config.ip());  
		this.meterType = config.type();

	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		this.calculateEnergy();
	    if (!this.isEnabled()) {
	        return;
	    }

	    switch (event.getTopic()) {
	    case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
	        this.fetchAndUpdateEssRealtimeStatus();
	        break;
	    }
	}
	


	private void fetchAndUpdateEssRealtimeStatus() {
	    try {
	        // Fetch the necessary data from the API
	        JsonObject necessaryData = samsungApi.getEssRealtimeStatus();

	        
	        // Populate the appropriate channels with the fetched data
	        double PvPw = necessaryData.get("PvPw").getAsDouble() * 1000;
	        
			this._setActivePower((int) PvPw);

	    } catch (OpenemsNamedException e) {
			this._setActivePower(0);
	        this.logError(log, "Failed to fetch Charger Real-time Status.");
	    }
	}
	
	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		var actualPower = this.getActivePower().get();
		if (actualPower == null) {
			// Not available
			this.calculateActualEnergy.update(null);
		} else if (actualPower > 0) {
			this.calculateActualEnergy.update(actualPower);
		} else {
			this.calculateActualEnergy.update(0);
		}
	}

	
	
	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString(); //
	}
	

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
	
	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}


}