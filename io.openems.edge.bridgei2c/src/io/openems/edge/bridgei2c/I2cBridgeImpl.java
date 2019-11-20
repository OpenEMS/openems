package io.openems.edge.bridgei2c;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.pi4j.io.gpio.GpioProviderBase;
import com.pi4j.io.gpio.Pin;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.bridgei2c.task.I2cTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.pwmModule.api.Pca9685GpioProvider;
import io.openems.edge.pwmModule.api.PcaGpioProvider;
import io.openems.edge.relaisboardmcp.Mcp;
import io.openems.edge.relaisboardmcp.Mcp23008;
import io.openems.edge.relaisboardmcp.Mcp4728;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;


@Designate(ocd = Config.class, factory = true)
@Component(name = "I2CBridge",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class I2cBridgeImpl extends AbstractOpenemsComponent implements OpenemsComponent, I2cBridge, EventHandler {

    private final I2cWorker worker = new I2cWorker();
    private List<Mcp> mcpList = new ArrayList<>();
    //String --> PwmModule
    private Map<String, PcaGpioProvider> gpioMap = new ConcurrentHashMap<>();
    //String --> PwmDevice
    private Map<String, I2cTask> tasks = new ConcurrentHashMap<>();
    private Pin[] pin;

    @Activate
    public void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        if (config.enabled()) {
            this.worker.activate(super.id());
        }
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
        this.worker.deactivate();
        //Relais will be default (depending on opener and closer) and Bhkw will be 0
        for (Mcp mcp : mcpList) {
            mcp.deactivate();
        }
        for (String id : this.gpioMap.keySet()) {
            removeGpioDevice(id);
        }
    }

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        ;

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    public I2cBridgeImpl() {
        super(OpenemsComponent.ChannelId.values());
    }

    @Override
    public void addMcp(Mcp mcp) {
        if (mcp != null) {
            for (Mcp existingMcp : this.mcpList) {
                if (existingMcp instanceof Mcp23008 && mcp instanceof Mcp23008) {
                    if (((Mcp23008) existingMcp).getParentCircuitBoard().equals(((Mcp23008) mcp).getParentCircuitBoard())) {
                        return;
                    }
                }
            }
            this.mcpList.add(mcp);
        }
    }

    @Override
    public List<Mcp> getMcpList() {
        return this.mcpList;
    }

    @Override
    public void removeMcp(Mcp toRemove) {
        Iterator<Mcp> iter = this.mcpList.iterator();
        while (iter.hasNext()) {
            Mcp willBeRemoved = iter.next();
            if (willBeRemoved instanceof Mcp23008 && toRemove instanceof Mcp23008) {
                if (((Mcp23008) willBeRemoved).getParentCircuitBoard().equals(((Mcp23008) toRemove).getParentCircuitBoard())) {
                    iter.remove();
                    break;
                }
            } else if (willBeRemoved instanceof Mcp4728 && toRemove instanceof Mcp4728) {
                if (((Mcp4728) willBeRemoved).getParentCircuitBoard().equals(((Mcp4728) toRemove).getParentCircuitBoard())) {
                    iter.remove();
                    break;
                }
            }
        }
    }

    @Override
    public void addGpioDevice(String id, PcaGpioProvider gpio) {
        if (!this.gpioMap.containsKey(id)) {
            this.gpioMap.put(id, gpio);
        } else {
            System.out.println("Warning! id " + id + "Already in Map, choose another id Name");
        }
    }

    @Override
    public void removeGpioDevice(String id) {

        for (I2cTask task : tasks.values()) {
            shutdown(task.getPwmModuleId());
        }
        this.gpioMap.remove(id);
    }

    @Override
    public void addI2cTask(String id, I2cTask i2cTask) throws OpenemsException {
        if (!this.tasks.containsKey(id)) {
            this.tasks.put(id, i2cTask);
        }
        else {
            throw new OpenemsException("Attention, id " + id + "is already Key, activate again with a new name");
        }
    }

    @Override
    public void removeI2cTask(String id) {
        shutdown(id);
        this.tasks.remove(id);
    }

    private void shutdown(String id) {
        PcaGpioProvider gpio = gpioMap.get(tasks.get(id).getPwmModuleId());
        if (gpio instanceof Pca9685GpioProvider) {

            if (tasks.get(id).isInverse()) {
                ((Pca9685GpioProvider) gpio).setAlwaysOn(tasks.get(id).getPinPosition());
            } else {
                ((Pca9685GpioProvider) gpio).setAlwaysOff(tasks.get(id).getPinPosition());
            }
        }

    }


    private class I2cWorker extends AbstractCycleWorker {
        @Override
        public void activate(String name) {
            super.activate(name);
        }

        @Override
        public void deactivate() {
            super.deactivate();
        }

        @Override
        public void forever() throws Throwable {
            for (Mcp mcp : getMcpList()) {
                if (mcp instanceof Mcp23008) {
                    ((Mcp23008) mcp).shift();
                } else if (mcp instanceof Mcp4728) {
                    ((Mcp4728) mcp).shift();
                }
            }
            for (I2cTask task : tasks.values()) {

                PcaGpioProvider gpio = gpioMap.get(task.getPwmModuleId());
                if (gpio instanceof Pca9685GpioProvider) {

                    //with or without offset?
                    int digit = task.calculateDigit(4096);
                    if (digit > 4095) {
                        digit = 4095;
                    } else if (digit < 0) {
                        digit = 0;
                    }
                    ((Pca9685GpioProvider) gpio).setPwm(task.getPinPosition(), 0, digit);
                }
            }
        }
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)) {
            this.worker.triggerNextRun();
        }
    }


}
