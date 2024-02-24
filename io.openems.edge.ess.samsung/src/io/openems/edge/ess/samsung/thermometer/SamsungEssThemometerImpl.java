package io.openems.edge.ess.samsung.thermometer;


import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
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
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.thermometer.api.Thermometer;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Thermometer.Samsung", immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE
)

@EventTopics({ //
	EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class SamsungEssThemometerImpl extends AbstractOpenemsComponent
		implements OpenemsComponent, EventHandler, Thermometer{

	
	private final Logger log = LoggerFactory.getLogger(SamsungEssThemometerImpl.class);
	private SamsungApi samsungApi = null;



	public SamsungEssThemometerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(),
				ManagedSymmetricPvInverter.ChannelId.values(),
				Thermometer.ChannelId.values()//
//
				//
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
	    super.activate(context, config.id(), config.alias(), config.enabled());
	    this.samsungApi = new SamsungApi(config.ip());
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
	        break;
	    }
	}
	


	private void fetchAndUpdateEssRealtimeStatus() {
	    try {
	        JsonObject weatherData = samsungApi.getEssWeatherStatus();

	        if (weatherData.has("Temperature")) {
	            double temperature = weatherData.get("Temperature").getAsDouble();
	            this._setTemperature((int) temperature * 10); // Use the appropriate setter method here
	        }

	        if (weatherData.has("Humidity")) {
	            int humidity = weatherData.get("Humidity").getAsInt();
	            this.channel(Thermometer.ChannelId.HUMIDITY).setNextValue(humidity);		
            }
	       

	    } catch (OpenemsNamedException e) {
	        this.logError(log, "Failed to fetch Charger Real-time Status.");
	    }
	}
	

	
	
	@Override
	public String debugLog() {
	    Integer tempValue = this.getTemperature().get();
	    Integer humidityValue = (Integer) this.channel(Thermometer.ChannelId.HUMIDITY).value().get();
	    return "temp[" + tempValue + " dC | humidity: " + humidityValue + " %]";
	}
	
}