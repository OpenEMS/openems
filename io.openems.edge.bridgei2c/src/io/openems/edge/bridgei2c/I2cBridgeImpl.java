package io.openems.edge.bridgei2c;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.bridgei2c.task.I2cTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.relaisboardmcp.Mcp;
import io.openems.edge.relaisboardmcp.Mcp23008;
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
    private final Map<String, I2cTask> tasks = new ConcurrentHashMap<>();


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
        for (Mcp mcp : this.mcpList) {
            if (mcp instanceof Mcp23008) {
                for (Map.Entry<Integer, Boolean> entry : ((Mcp23008) mcp).getValuesPerDefault().entrySet()) {
                    ((Mcp23008) mcp).setPosition(entry.getKey(), entry.getValue());
                }
                ((Mcp23008) mcp).shift();
            }
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


    public I2cBridgeImpl() {
        super(OpenemsComponent.ChannelId.values());

    }


    @Override
    public void addTask(String id, I2cTask task) {
        this.tasks.put(id, task);
    }

    @Override
    public void removeTask(String id) {
        this.tasks.get(id).deactivate();
        this.tasks.remove(id);
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
            for (I2cTask task : tasks.values()) {
                Optional<Boolean> optional;
                do {
                    optional = task.getWriteChannel().getNextWriteValueAndReset();
                    optional.ifPresent(aBoolean -> task.getReadChannel().setNextValue(aBoolean));
                } while (optional.isPresent());

                boolean high = task.isReverse() != task.isActive();
                for (Mcp existingMcp : getMcpList()) {

                    if (existingMcp instanceof Mcp23008) {
                        if (((Mcp23008) existingMcp).getParentCircuitBoard().equals(task.getRelaisBoard())) {
                            ((Mcp23008) existingMcp).setPosition(task.getPosition(), high);
                            break;
                        }
                    }
                }
            }
            for (Mcp mcp : getMcpList()) {
                if (mcp instanceof Mcp23008) {
                    ((Mcp23008) mcp).shift();
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
