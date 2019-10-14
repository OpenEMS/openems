package io.openems.edge.raspberrypi.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.pi4j.wiringpi.Spi;
import io.openems.edge.raspberrypi.circuitboard.CircuitBoard;
import io.openems.edge.raspberrypi.spi.task.Task;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.common.event.EdgeEventConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Designate(ocd = Config.class, factory = true)
@Component(name = "SpiInitial",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class SpiInitialImpl  implements SpiInitial, EventHandler {
    private final Logger log = LoggerFactory.getLogger(SpiInitialImpl.class);
    private List<CircuitBoard> circuitBoards = new ArrayList<>();
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private final SpiWorker worker = new SpiWorker();

    @Activate
    public void activate(Config config) {
        if (config.enabled()) {
            this.worker.activate(config.id());
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
                byte[] data = task.getRequest();
                int uebergabe = task.getSpiChannel();
                Spi.wiringPiSPIDataRW(uebergabe,data);
                task.setResponse(data);
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

}
