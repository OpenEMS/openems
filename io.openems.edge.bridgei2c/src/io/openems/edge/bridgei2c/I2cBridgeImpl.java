package io.openems.edge.bridgei2c;

import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.bridgei2c.task.I2cTask;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.relaisBoard.RelaisBoardImpl;
import io.openems.edge.relaisBoard.api.Mcp23008;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


@Designate(ocd = Config.class, factory = true)
@Component(name = "I2C Bridge",
immediate = true,
configurationPolicy = ConfigurationPolicy.REQUIRE,
property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class I2cBridgeImpl extends AbstractOpenemsComponent implements OpenemsComponent, I2cBridge {

    private final I2cWorker worker = new I2cWorker();
    private List<RelaisBoardImpl> relaisBoards = new ArrayList<>();
    private final Map<String, I2cTask> tasks = new ConcurrentHashMap<>();

    public I2cBridgeImpl() {
        super(OpenemsComponent.ChannelId.values());

    }

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
        for (I2cTask task : tasks.values()) {

            if (task.isActive()) {
                task.deactivate();
                return;
            } else {
                task.deactivate();
                return;
            }
        }
        for (RelaisBoardImpl relais : this.relaisBoards) {
            relais.deactivate();
        }
        this.worker.deactivate();

    }

    @Override
    public void addTask(String id, I2cTask task) {
        this.tasks.put(id, task);
    }

    @Override
    public void removeTask(String id) {
        this.tasks.remove(id);
    }

    @Override
    public void addRelaisBoard(RelaisBoardImpl relaisBoard) {
        if (relaisBoard != null) {
            for (RelaisBoardImpl relais : this.relaisBoards) {
                if (relais.getId().equals(relaisBoard.getId())) {
                    return;
                }
            }
            this.relaisBoards.add(relaisBoard);
        }

    }

    @Override
    public List<RelaisBoardImpl> getRelaisBoardList() {
        return this.relaisBoards;
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
            for (I2cTask task : tasks.values()) {
                Optional<Boolean> optional;
                do {
                    optional = task.getWriteChannel().getNextWriteValueAndReset();
                    optional.ifPresent(aBoolean -> task.getReadChannel().setNextValue(aBoolean));
                } while (optional.isPresent());

                boolean high = task.isReverse() != task.isActive();
				for (RelaisBoardImpl relais : getRelaisBoardList()) {
					if (task.getRelaisBoard().equals(relais.getId())) {
						if (relais.getMcp() instanceof Mcp23008) {
							((Mcp23008) relais.getMcp()).setPosition(task.getPosition(), high);
						}
					}
				}
			}
		}
    }


}
