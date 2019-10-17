package io.openems.edge.raspberrypi.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.pi4j.wiringpi.Spi;
import io.openems.common.types.EdgeConfig;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.raspberrypi.circuitboard.CircuitBoard;
import io.openems.edge.raspberrypi.spi.task.Task;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.common.event.EdgeEventConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Spi Initial",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS)
public class SpiInitialImpl extends AbstractOpenemsComponent implements SpiInitial, EventHandler, OpenemsComponent, SpiBridge {
    @Reference
    ConfigurationAdmin cm;

    private final Logger log = LoggerFactory.getLogger(SpiInitialImpl.class);

    private List<CircuitBoard> circuitBoards = new ArrayList<>();
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private final SpiWorker worker = new SpiWorker();

    @Activate
    public void activate(ComponentContext context, Config config) {
//        if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "Spi", config.id())) {
//            return;
//        }
        super.activate(context, config.id(), config.alias(), config.enabled());

        if (config.enabled()) {
            this.worker.activate(super.id());
        }
    }

    @Deactivate
    public void deactivate() {
        for (CircuitBoard circuitBoard : circuitBoards
        ) {
            circuitBoard.deactivate();
        }
        this.worker.deactivate();
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

    public SpiInitialImpl() {
        //super(OpenemsComponent.ChannelId.values(), SpiBridge.ChannelId.values());
        super(OpenemsComponent.ChannelId.values());
    }

    @Override
    public void addTask(String sourceId, Task task) {
        this.tasks.put(sourceId, task);
    }

    @Override
    public void removeTask(String sourceId) {
        this.tasks.remove(sourceId);
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
            boolean implemented = false;
            for (Task task : tasks.values()) {
                // byte[] data = task.getRequest();
                // int channelInput = task.getSpiChannel();
                // Spi.wiringPiSPIDataRW(channelInput,data);
                //task.setResponse(data);
                implemented = true;
                log.info("Task implemented");
            }
            if(!implemented) {
                log.info("No Tasks implemented yet");
            }
        }
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS)) {
            this.worker.triggerNextRun();
        }
    }

    @Override
    public List<CircuitBoard> getCircuitBoards() {
        return this.circuitBoards;
    }

    @Override
    public void addCircuitBoards(CircuitBoard cb) {
        if (cb != null) {
            for (CircuitBoard board : this.circuitBoards) {
                if (board.getCircuitBoardId().equals(cb.getCircuitBoardId())) {
                    return;
                } else {
                    this.circuitBoards.add(cb);
                    return;
                }
            }
            this.circuitBoards.add(cb);
        }
    }

    @Override
    public void removeCircuitBoard(CircuitBoard circuitBoard) {
        for (CircuitBoard rbToRemove : this.circuitBoards) {
            if (rbToRemove.getCircuitBoardId().equals(circuitBoard.getCircuitBoardId())) {
                this.circuitBoards.remove(rbToRemove);
                break;
            }
        }
    }


}


