package io.openems.edge.manager.valve;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.i2c.api.I2cBridge;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.heatsystem.components.api.Valve;
import io.openems.edge.i2c.mcp.api.McpChannelRegister;
import io.openems.edge.manager.valve.api.ManagerValve;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Consolinno.Manager.Valve",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ManagerValveImpl extends AbstractOpenemsComponent implements OpenemsComponent, Controller, ManagerValve {
    @Reference(policy = ReferencePolicy.STATIC,
            policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    I2cBridge i2cBridge;


    private Map<String, Valve> valves = new ConcurrentHashMap<>();

    //private final static int PERCENT_TOLERANCE_VALVE = 5;

    public ManagerValveImpl() {
        super(OpenemsComponent.ChannelId.values(), Controller.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Override
    public void addValve(String id, Valve valve) {
        this.valves.put(id, valve);

    }

    @Override
    public void removeValve(String id) {
        this.valves.remove(id);
    }

    @Override
    public void run() throws OpenemsError.OpenemsNamedException {
        valves.values().forEach(valve -> {
            //next Value bc on short scheduler the value.get() is not quick enough updated
            //Should valve be Reset?
            if (valve.shouldReset()) {
                valve.reset();
                valve.shouldForceClose().setNextValue(false);
                valve.updatePowerLevel();
            } else {
                //Reacting to SetPowerLevelPercent by REST Request
                if (valve.setPowerLevelPercent().value().isDefined() && valve.setPowerLevelPercent().value().get() >= 0) {

                    int changeByPercent = valve.setPowerLevelPercent().value().get();
                    //getNextPowerLevel Bc it's the true current state that's been calculated
                    if (valve.getPowerLevel().getNextValue().isDefined()) {
                        changeByPercent -= valve.getPowerLevel().getNextValue().get();
                    }
                    if (valve.changeByPercentage(changeByPercent)) {
                        valve.setPowerLevelPercent().setNextValue(-1);
                    }
                }
                //Calculate current % State of Valve
                if (valve.powerLevelReached()) {
              /*  double valvePowerLevel = valve.setGoalPowerLevel().getNextValue().get() - valve.getPowerLevel().value().get();
                if (Math.abs(valvePowerLevel) > PERCENT_TOLERANCE_VALVE) {
                    valve.changeByPercentage(valvePowerLevel);
                }*/
                } else {
                    valve.updatePowerLevel();
                }
            }
        });
        i2cBridge.getMcpList().forEach(McpChannelRegister::shift);
    }
}
