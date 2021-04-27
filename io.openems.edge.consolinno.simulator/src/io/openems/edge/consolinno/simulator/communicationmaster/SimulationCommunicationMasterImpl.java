package io.openems.edge.consolinno.simulator.communicationmaster;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.api.HydraulicLineHeater;
import io.openems.edge.heater.decentral.api.DecentralHeater;
import org.joda.time.DateTime;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Simulation.CommunicationMaster",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true
)
public class SimulationCommunicationMasterImpl extends AbstractOpenemsComponent implements OpenemsComponent, Controller {

    @Reference
    ComponentManager cpm;

    Random random = new Random();
    private int maxRequests;
    private final AtomicInteger currentRequests = new AtomicInteger();
    Map<DecentralHeater, Boolean> isManaged = new HashMap<>();
    List<DecentralHeater> managedHeaterList = new ArrayList<>();
    Map<DecentralHeater, DateTime> workMap = new HashMap<>();
    private static final int MIN_WORK_TIME_IN_SECONDS = 20;

    private boolean useHeater;
    private boolean useHydraulicLineHeater;
    private final List<DecentralHeater> decentralHeaterList = new ArrayList<>();
    private HydraulicLineHeater hydraulicLineHeater;

    public SimulationCommunicationMasterImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        OpenemsError.OpenemsNamedException[] exNamed = {null};
        ConfigurationException[] exConfig = {null};
        if (config.useHeater()) {
            List<String> decentralHeaterStrings = Arrays.asList(config.decentralHeaterIds());
            decentralHeaterStrings.forEach(entry -> {
                if (exNamed[0] == null && exConfig[0] == null) {
                    OpenemsComponent component;
                    try {
                        component = cpm.getComponent(entry);
                        if (component instanceof DecentralHeater) {
                            this.decentralHeaterList.add((DecentralHeater) component);
                        } else {
                            exConfig[0] = new ConfigurationException("Activate: SimulationCommunicationMasterImpl", component.id() + " not a DecentralHeater!");
                        }
                    } catch (OpenemsError.OpenemsNamedException e) {
                        exNamed[0] = e;
                    }

                }
            });
        }
        if (config.useHydraulicLineHeater()) {
            OpenemsComponent lineHeater = cpm.getComponent(config.hydraulicLineHeaterId());
            if (lineHeater instanceof HydraulicLineHeater) {
                this.hydraulicLineHeater = (HydraulicLineHeater) lineHeater;
            } else {
                throw new ConfigurationException("ActivateMethod SimulationCommunicationMaster", "HydraulicLineHeaterId not correct" + lineHeater.id());
            }
        }
        if (exNamed[0] != null) {
            throw exNamed[0];
        }
        if (exConfig[0] != null) {
            throw exConfig[0];
        }
        this.maxRequests = config.maxSize();
        this.useHeater = config.useHeater();
        this.useHydraulicLineHeater = config.useHydraulicLineHeater();
    }


    @Override
    public void run() throws OpenemsError.OpenemsNamedException {
        checkMissingComponent();
        AtomicBoolean atLeastOneRequest = new AtomicBoolean(false);
        if (this.useHeater) {
            this.decentralHeaterList.forEach(decentralHeater -> {
                if (decentralHeater.getNeedHeat()) {
                    atLeastOneRequest.set(true);
                    currentRequests.getAndIncrement();
                    if (this.managedHeaterList.size() < this.maxRequests) {
                        //say with 99% probability it's ok to write true --> else false --> some randomness in tests
                        enableOrDisableHeater(decentralHeater, random.nextInt(100) < 99);
                    } else {
                        enableOrDisableHeater(decentralHeater, false);
                    }
                } else {
                    enableOrDisableHeater(decentralHeater, false);
                }

            });
            //More Requests than allowed ---> some are awaiting enabledSignal
            if (currentRequests.get() > this.maxRequests && random.nextBoolean()) {
                DateTime now = new DateTime();
                this.isManaged.forEach((key, value) -> {
                    if (value) {
                        if (now.isAfter(this.workMap.get(key).plusSeconds(MIN_WORK_TIME_IN_SECONDS))) {
                            this.isManaged.replace(key, random.nextBoolean());
                        }
                    }
                    if (value == false) {
                        this.managedHeaterList.remove(key);
                        try {
                            key.getNeedHeatEnableSignalChannel().setNextWriteValue(false);
                        } catch (OpenemsError.OpenemsNamedException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
        if (this.useHydraulicLineHeater) {
            boolean enableSignal = atLeastOneRequest.get() && random.nextInt(100) < 90;
            this.hydraulicLineHeater.enableSignal().setNextWriteValue(enableSignal);
        }
    }

    private void enableOrDisableHeater(DecentralHeater decentralHeater, boolean enable) {
        try {
            this.isManaged.put(decentralHeater, enable);
            decentralHeater.getNeedHeatEnableSignalChannel().setNextWriteValue(enable);
            if (enable) {
                this.managedHeaterList.add(decentralHeater);
                this.workMap.put(decentralHeater, new DateTime());
            } else {
                this.managedHeaterList.remove(decentralHeater);
            }
        } catch (OpenemsError.OpenemsNamedException ignored) {

        }
    }

    private void checkMissingComponent() {
        if (this.hydraulicLineHeater != null && this.hydraulicLineHeater.isEnabled() == false) {
            try {
                this.hydraulicLineHeater = cpm.getComponent(this.hydraulicLineHeater.id());
            } catch (OpenemsError.OpenemsNamedException e) {
                e.printStackTrace();
            }
            List<DecentralHeater> missingHeater = this.decentralHeaterList.stream().filter(heater -> heater.isEnabled() == false).collect(Collectors.toList());
            missingHeater.forEach(missing -> {
                try {
                    this.decentralHeaterList.set(this.decentralHeaterList.indexOf(missing), cpm.getComponent(missing.id()));
                } catch (OpenemsError.OpenemsNamedException e) {
                    e.printStackTrace();
                }
            });
        }
    }

}
