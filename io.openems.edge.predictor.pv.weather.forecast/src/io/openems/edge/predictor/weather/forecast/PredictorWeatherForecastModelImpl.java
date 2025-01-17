package io.openems.edge.predictor.weather.forecast;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.edge.predictor.api.prediction.Prediction.EMPTY_PREDICTION;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.stream.Collectors;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.predictor.api.prediction.AbstractPredictor;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.prediction.Predictor;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
        name = "Predictor.WeatherForecastMode", //
        immediate = true, //
        configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class PredictorWeatherForecastModelImpl extends AbstractPredictor implements Predictor, OpenemsComponent {

    private final Logger log = LoggerFactory.getLogger(PredictorWeatherForecastModelImpl.class);
    private double factor;  // Factor to multiply with short wave solar radiation to forecast PV production

    @Reference
    private ComponentManager componentManager;

    private Config config;
    private OpenMeteoForecast openMeteoForecast;  // Service to fetch weather data

    public PredictorWeatherForecastModelImpl() throws OpenemsNamedException {
        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values(),
                PredictorWeatherForecastModel.ChannelId.values());
    }

    @Activate
    private void activate(ComponentContext context, Config config) throws Exception {
        this.config = config;
        super.activate(context, this.config.id(), this.config.alias(), this.config.enabled(),
                this.config.channelAddresses(), this.config.logVerbosity());
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    protected ClockProvider getClockProvider() {
        return this.componentManager;
    }

    protected Prediction createNewPrediction(ChannelAddress channelAddress) {
        try {
            // Fetch latest weather forecast data every 15 minutes
            this.openMeteoForecast = new OpenMeteoForecast();  // initialize here 
            this.openMeteoForecast.fetchData(this.config.latitude(), this.config.longitude());  // Fetch the weather forecast data from API
            this.factor = config.factor();  // Factor for calculating PV production

            Optional<List<Double>> shortWaveRadiationOpt = openMeteoForecast.getShortWaveRadiation();

            // If we don't have data, return empty prediction
            if (shortWaveRadiationOpt.isEmpty()) {
                return Prediction.EMPTY_PREDICTION;
            }

            List<Double> shortwaveRadiation = shortWaveRadiationOpt.get();

            // If the data size is less than 192, return empty prediction
            if (shortwaveRadiation.size() < 192) {
                return Prediction.EMPTY_PREDICTION;
            }

            // Get the current time
            ZonedDateTime now = ZonedDateTime.now(this.componentManager.getClock());  
            ZonedDateTime startOfDay = now.truncatedTo(ChronoUnit.DAYS);  // Start of the current day

            // Calculate the index corresponding to the current 15-minute interval
            int currentIntervalIndex = (int) ChronoUnit.MINUTES.between(startOfDay, now) / 15;  // Index of current 15-minute interval

            // Ensure the currentIntervalIndex is within bounds
            if (currentIntervalIndex >= shortwaveRadiation.size()) {
                return Prediction.EMPTY_PREDICTION;
            }

            // Create an array to store the forecast values for the next 192 intervals (48 hours in 15-minute steps)
            var values = new Integer[192];

            // Extract data starting from the calculated currentIntervalIndex
            for (int i = 0; i < 192; i++) {
                int dataIndex = currentIntervalIndex + i;  // Get the data index for each 15-minute interval
                values[i] = dataIndex < shortwaveRadiation.size() ?
                        (int) Math.round(shortwaveRadiation.get(dataIndex) * this.factor) : 0;  // Forecast PV production based on radiation data
            }

            // Return the prediction starting from the calculated time
            return Prediction.from(startOfDay.plusMinutes(currentIntervalIndex * 15), values);

        } catch (Exception e) {
            log.error("Error creating prediction: ", e);
            return Prediction.EMPTY_PREDICTION;
        }
    }
}
