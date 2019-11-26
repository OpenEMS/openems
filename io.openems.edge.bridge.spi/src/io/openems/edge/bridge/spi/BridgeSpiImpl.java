package io.openems.edge.bridge.spi;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.bridge.spi.task.SpiTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.temperature.board.api.Adc;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pi4j.wiringpi.Spi;

@Designate(ocd = Config.class, factory = true)
@Component(name = "SpiInitial",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS)
public class BridgeSpiImpl extends AbstractOpenemsComponent implements BridgeSpi, EventHandler, OpenemsComponent, SpiBridgeChannel {

    private final Logger log = LoggerFactory.getLogger(BridgeSpiImpl.class);

    //private List<CircuitBoard> circuitBoards = new ArrayList<>();
    //private final Map<String, Task> tasks = new ConcurrentHashMap<>();

    private Set<Adc> adcList = new HashSet<>();
    private final SpiWorker worker = new SpiWorker();
    private Map<String, SpiTask> tasks = new ConcurrentHashMap<>();

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
        adcList.forEach(this::removeAdc);
    }

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        ;

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    public BridgeSpiImpl() {
        super(OpenemsComponent.ChannelId.values());
    }

    @Override
    public void addAdc(Adc adc) {
        if (adc != null) {
            this.adcList.add(adc);
        }
    }


    @Override
    public Set<Adc> getAdcs() {
        return this.adcList;
    }

    @Override
    public void removeAdc(Adc adc) {
        tasks.values().removeIf(value -> value.getSpiChannel() == adc.getSpiChannel());
        this.adcList.removeIf(value -> value.getCircuitBoardId().equals(adc.getCircuitBoardId()));
        adc.deactivate();
    }

    @Override
    public void addSpiTask(String id, SpiTask spiTask) throws ConfigurationException {
        if (!this.tasks.containsKey(id)) {
            this.tasks.put(id, spiTask);
        } else {
            throw new ConfigurationException("Attention, id of Sensor : " + id
                    + "is already a Key, activate Sensor again with another id ",
                    "Type another Id name in your configuration");
        }
    }

    @Override
    public void removeSpiTask(String id) {
        this.tasks.remove(id);
    }


    private class SpiWorker extends AbstractCycleWorker {

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
            tasks.values().forEach(task -> {
                byte[] data = task.getRequest();
                Spi.wiringPiSPIDataRW(task.getSpiChannel(), data);
                task.setResponse(data);
            });
        }
    }

    public Map<String, SpiTask> getTasks() {
        return tasks;
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS)) {
            this.worker.triggerNextRun();
        }
    }
}


