package io.openems.edge.ess.samsung.gridmeter;


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
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Grid-Meter.Samsung", immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE
)

@EventTopics({ //
	EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class SamsungEssGridmeterImpl extends AbstractOpenemsComponent
		implements SamsungEssGridmeter, ElectricityMeter, OpenemsComponent, EventHandler, TimedataProvider{

	private String currentGridStatus = "Unknown";

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
	        ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
	        ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	
	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;
	
	
	private final Logger log = LoggerFactory.getLogger(SamsungEssGridmeterImpl.class);
	private SamsungApi samsungApi = null;
	private MeterType meterType = null;



	public SamsungEssGridmeterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values()
//
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
	    if (!this.isEnabled()) {
	        return;
	    }

	    switch (event.getTopic()) {
	    case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
	        this.fetchAndUpdateEssRealtimeStatus();
	        this.calculateEnergy();  // Call the calculateEnergy method here
	        break;
	    }
	}

	


	private void fetchAndUpdateEssRealtimeStatus() {
	    try {
	        // Fetch the necessary data from the API
	        JsonObject necessaryData = samsungApi.getEssRealtimeStatus();

	        // Populate the appropriate channels with the fetched data
	        double gridPw = necessaryData.get("GridPw").getAsDouble() * 1000;
	        int GridStusCd = necessaryData.get("GridStusCd").getAsInt();

	        // Handle different cases based on GridStusCd
	        switch (GridStusCd) {
	            case 0:
	            	//Buy from Grid is positive
	                this.currentGridStatus = "Buy from Grid";
	                break;

	            case 1:
	            	//Sell to Grid is negative
	                gridPw = -gridPw;
	                this.currentGridStatus = "Sell to Grid";

	                break;
	            default:
	                // Handle unknown status codes if needed
	                this.currentGridStatus = "Unknown";
	                gridPw = 0;
	                this.logWarn(log, "Unknown Grid Status Code: " + GridStusCd);
	        }

	        // Update the active power
	        this._setActivePower((int) gridPw);

	    } catch (OpenemsNamedException e) {
	        this.logError(log, "Failed to fetch Grid Meter Real-time Status");
	    }
	}


	private void calculateEnergy() {
	    Integer activePower = this.getActivePower().orElse(null);
	    if (activePower == null) {
	        // Not available
	        this.calculateProductionEnergy.update(null);
	        this.calculateConsumptionEnergy.update(null);
	    } else if (activePower > 0) {
	        // Buy-From-Grid
	        this.calculateProductionEnergy.update(activePower);
	        this.calculateConsumptionEnergy.update(0);
	    } else {
	        // Sell-To-Grid
	        this.calculateProductionEnergy.update(0);
	        this.calculateConsumptionEnergy.update(-activePower);
	    }
	}


	
	
	@Override
	public String debugLog() {
	    return "|L:" + this.getActivePower().asString() + " |Status: " + this.currentGridStatus;
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