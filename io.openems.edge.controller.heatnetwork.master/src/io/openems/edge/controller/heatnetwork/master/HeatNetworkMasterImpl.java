package io.openems.edge.controller.heatnetwork.master;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.master.api.HeatNetworkMaster;
import io.openems.edge.controller.heatnetwork.controlcenter.api.ControlCenter;
import io.openems.edge.remote.rest.device.api.RestRemoteDevice;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Heatnetwork.Master")
public class HeatNetworkMasterImpl extends AbstractOpenemsComponent implements OpenemsComponent, Controller, HeatNetworkMaster {

    @Reference
    ComponentManager cpm;

    private List<RestRemoteDevice> heatTankRequests = new ArrayList<>();
    private List<RestRemoteDevice> heatNetworkReady = new ArrayList<>();
    private ControlCenter allocatedController;
    private int lastTemperature;


    public HeatNetworkMasterImpl() {
        super(OpenemsComponent.ChannelId.values(),
                HeatNetworkMaster.ChannelId.values(),
                Controller.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        AtomicBoolean instanceFound = new AtomicBoolean(false);

        cpm.getAllComponents().stream().filter(component -> component.id().equals(config.id())).findFirst().ifPresent(consumer -> {
            instanceFound.set(true);
        });
        if (instanceFound.get() == true) {
            return;
        }
        super.activate(context, config.id(), config.alias(), config.enabled());
        OpenemsError.OpenemsNamedException[] ex = {null};
        ConfigurationException[] exC = {null};
        //Configure all Remote Components; Listen to Heat-requests
        Arrays.stream(config.requests()).forEach(consumer -> {
            try {
                if (cpm.getComponent(consumer) instanceof RestRemoteDevice) {
                    this.heatTankRequests.add(cpm.getComponent(consumer));
                } else {
                    throw new ConfigurationException("Wrong Component Name", "RestReadRemote Devices Wrong");
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                ex[0] = e;
            } catch (ConfigurationException e) {
                exC[0] = e;
            }
        });
        //Configure all Remote components; Listening to Heatnetwork ready.
        Arrays.stream(config.readyResponse()).forEach(consumer -> {
            try {
                if (cpm.getComponent(consumer) instanceof RestRemoteDevice) {
                    this.heatNetworkReady.add(cpm.getComponent(consumer));
                } else {
                    throw new ConfigurationException("Wrong Component", "RestWriteRemote Device is Wrong.");
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                ex[0] = e;
            } catch (ConfigurationException e) {
                exC[0] = e;
            }
        });
        //Throw Exceptions if any occurred during configuration of Remote Devices.
        if (ex[0] != null) {
            throw ex[0];
        } else if (exC[0] != null) {
            throw exC[0];
        }
        //Main Heat Control-Center reacting to this Controller
        if (cpm.getComponent(config.allocatedController()) instanceof ControlCenter) {
            this.allocatedController = cpm.getComponent(config.allocatedController());
        }
        this.temperatureSetPointChannel().setNextValue(config.temperatureSetPoint());
        this.allocatedController.setOverrideTemperature().setNextWriteValue(config.temperatureSetPoint());
        this.allocatedController.activateTemperatureOverride().setNextWriteValue(false);
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();

    }

    @Reference
    ConfigurationAdmin ca;

    private void updateConfig() {
        Configuration c;

        try {
            int channelTemp = this.temperatureSetPointChannel().value().get();
            c = ca.getConfiguration(this.servicePid(), "?");
            Dictionary<String, Object> properties = c.getProperties();
            int setPointTemperature = (int) properties.get("temperatureSetPoint");
            if (setPointTemperature != channelTemp) {
                properties.put("temperatureSetPoint", channelTemp);
                c.update(properties);
            }
        } catch (IOException e) {
        }
    }

    @Override
    public void run() throws OpenemsError.OpenemsNamedException {
        //NO DEMAND!
        // Equals 1 because Rest Get request returns 0 or 1 at boolean
        if (this.temperatureSetPointChannel().getNextWriteValueAndReset().isPresent()) {
            updateConfig();
        }
        if (this.heatTankRequests.stream().noneMatch(consumer -> consumer.getValue().equals("1"))) {
            this.heatNetworkReady.forEach(consumer -> consumer.setValue("false"));
            this.lastTemperature = -1;
            this.allocatedController.activateTemperatureOverride().setNextWriteValue(false);

        } else {
            //Check if temperatureSetPoint is defined --> Can be changed outside of config --> e.g. REST Post
            if (this.temperatureSetPointChannel().value().isDefined()) {
                if (this.allocatedController.activateTemperatureOverride().value().isDefined()) {
                    boolean controlCenterIsActive = this.allocatedController.activateTemperatureOverride().value().get();
                    if (controlCenterIsActive == false) {
                        //Activate temperature override and set Temperature
                        this.allocatedController.activateTemperatureOverride().setNextValue(true);
                        this.allocatedController.setOverrideTemperature().setNextWriteValue(this.temperatureSetPointChannel().value().get());
                        //Notify all Remote Devices that Heatnetwork is ready
                        this.heatNetworkReady.forEach(consumer -> consumer.setValue("true"));
                        lastTemperature = this.temperatureSetPointChannel().value().get();
                        return;
                        //If Controlcenter is active but SetPointTemperature has changed.
                    } else if (controlCenterIsActive == true) {
                        this.allocatedController.activateTemperatureOverride().setNextValue(true);
                        this.allocatedController.setOverrideTemperature().setNextWriteValue(temperatureSetPointChannel().value().get());
                        lastTemperature = this.temperatureSetPointChannel().value().get();

                    }
                }
            }
        }

    }
}
