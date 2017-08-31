package io.openems.impl.controller.symmetric.avoidtotalcharge;

/**
 * Created by maxo2 on 29.08.2017.
 */
import java.util.*;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.device.nature.meter.MeterNature;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.api.security.User;
import io.openems.impl.controller.symmetric.avoidtotalcharge.Ess.State;
import io.openems.impl.controller.symmetric.balancing.Meter;

@ThingInfo(title = "Avoid total charge of battery. (Symmetric)", description = "Provides control over the battery's maximum state of charge at a specific time of day. For symmetric Ess.")
public class AvoidTotalChargeController extends Controller {

    /*
     * Config
     */
    @ConfigInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class, isArray = true)
    public final ConfigChannel<Set<Ess>> esss = new ConfigChannel<Set<Ess>>("esss", this);

    @ConfigInfo(title = "Grid Meter", description = "Sets the grid meter.", type = io.openems.impl.controller.symmetric.avoidtotalcharge.Meter.class, isOptional = false, isArray = false)
    public final ConfigChannel<io.openems.impl.controller.symmetric.avoidtotalcharge.Meter> gridMeter = new ConfigChannel<>("gridMeter", this);

    @ConfigInfo(title = "Production Meters", description = "Sets the production meter.", type = io.openems.impl.controller.symmetric.avoidtotalcharge.Meter.class, isOptional = false, isArray = true)
    public final ConfigChannel<Set<io.openems.impl.controller.symmetric.avoidtotalcharge.Meter>> productionMeters = new ConfigChannel<>("productionMeters", this);

    @ConfigInfo(title = "Graph", description = "Sets the socMaxVals.", type = Long[].class, isArray = true, accessLevel = User.OWNER)
    public final ConfigChannel<Long[]> graph = new ConfigChannel<>("graph", this);
    //TODO: implement fixed length and min/max values (accessible by OWNER !)

    @ConfigInfo(title = "Critical Percentage", description = "If the productionMeter's power raises above this percentage of its peak value, the graph-value may be neglected.", type = Long.class, accessLevel = User.OWNER, defaultValue = "100", isArray = false)
    public final ConfigChannel<Long> criticalPercentage = new ConfigChannel<Long>("criticalPercentage", this);
    //TODO: implement min/max values (accessible by OWNER !)

    /*
     * Constructors
     */
    public AvoidTotalChargeController() {
        super();
    }

    public AvoidTotalChargeController(String thingId) {
        super(thingId);
    }

    /*
     * Methods
     */
    @Override
    public void run() {

        try {
            /**
             * calculate the average available charging power
             */
            Long avgMinActivePower = 0L;

            for (Ess ess : esss.value()) {
                avgMinActivePower += ess.allowedCharge.value();
            }
            avgMinActivePower = avgMinActivePower / esss.value().size();

            for (Ess ess : esss.value()) {

                /**
                 * generate ChargingGraph and get maxWantedSoc value
                 */
                Map<Integer, Double> m = new HashMap<Integer, Double>(0);
                for (int i = 0; i < 24; i++) {
                    m.put(i, new Double((double) graph.value()[i] / 100.0));
                }
                ManualGraph mg = new ManualGraph(m);
                Long maxWantedSoc = (long) (100 * mg.getCurrentVal());

                /**
                 * get the power relatively produced to the producer's peak value
                 */
                Long maxAbsoluteProducedPower = 0L;
                Long relativeProducedPower = 0L;
                Long absoluteProducedPower = 0L;

                for (io.openems.impl.controller.symmetric.avoidtotalcharge.Meter meter : productionMeters.value()){
                    absoluteProducedPower += meter.activePower.value();
                    maxAbsoluteProducedPower += meter.maxActivePower.value();
                }

                relativeProducedPower = 100 * absoluteProducedPower / maxAbsoluteProducedPower;


                /**
                 * check if state of charge is above the value specified by the user and deny charging in
                 * case. However, in case the critical percentage was reached by the average
                 * relativeProducedPower and the spareProducedPower is negative (-> grid is taking the
                 * maxFeedablePower) allow charging nevertheless.
                 */
                if (ess.soc.value() >= maxWantedSoc) {
                    if(relativeProducedPower >= criticalPercentage.value()) {
                        double factor = (double) criticalPercentage.value() / (double) 100;
                        double maxFeedablePower = factor * (double) maxAbsoluteProducedPower;
                        double av = (double) gridMeter.value().activePower.value();
                        Long spareProducedPower = (long) (av + maxFeedablePower);
                        if (spareProducedPower < 0L){
                            try {
                                Long minValue = (ess.allowedCharge.value() / avgMinActivePower) * (spareProducedPower / esss.value().size());
                                ess.setActivePower.pushWriteMin(minValue);
                                ess.setActivePower.pushWriteMax(0L);
                            } catch (Exception e) {
                                log.error(e.getMessage(),e);
                            }
                        } else {
                            try {
                                ess.setActivePower.pushWriteMin(0L);
                            } catch (Exception e) {
                                log.error(e.getMessage(),e);
                            }
                        }
                    } else {
                        try {
                            ess.setActivePower.pushWriteMin(0L);
                        } catch (Exception e) {
                            log.error(e.getMessage(),e);
                        }
                    }
                }
            }

        } catch (InvalidValueException | IndexOutOfBoundsException e){
            log.error(e.getMessage(),e);
        }
    }
}