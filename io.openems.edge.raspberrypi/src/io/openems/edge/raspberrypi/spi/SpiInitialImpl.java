package io.openems.edge.raspberrypi.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.pi4j.wiringpi.Spi;
import io.openems.edge.raspberrypi.circuitboard.CircuitBoard;
import io.openems.edge.raspberrypi.spi.task.Task;
import org.osgi.service.cm.ConfigurationAdmin;
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
@Component(name = "Spi.Initial",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class SpiInitialImpl  implements SpiInitial, EventHandler {
    private final Logger log = LoggerFactory.getLogger(SpiInitialImpl.class);
    private List<CircuitBoard> circuitBoards = new ArrayList<>();
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private final SpiWorker worker = new SpiWorker();
    private Config config;
@Reference
    ConfigurationAdmin cm;
    @Activate
    public void activate(Config config) {
        this.config = config;
        if (this.config.enabled()) {
            this.worker.activate(this.config.id());
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
        protected void forever() throws Throwable {
            for (Task task : tasks.values()) {
              //  byte[] data = task.getRequest();
                int channelInput = task.getSpiChannel();
              //  Spi.wiringPiSPIDataRW(channelInput,data);
               // task.setResponse(data);
            }
        }
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)) {
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


