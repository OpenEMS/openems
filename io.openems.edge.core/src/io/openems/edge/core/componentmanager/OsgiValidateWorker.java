package io.openems.edge.core.componentmanager;

import java.util.concurrent.atomic.AtomicBoolean;

import io.openems.edge.common.component.ComponentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.worker.AbstractWorker;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * This Worker constantly validates if all configured OpenEMS-Components are
 * actually activated. If not it prints a warning message ("Component [ID] is
 * configured but not active!") and sets the
 * {@link ComponentManager.ChannelId#CONFIG_NOT_ACTIVATED} StateChannel.
 */
public class OsgiValidateWorker extends AbstractWorker {

    /*
     * For INITIAL_CYCLES cycles the distance between two checks is
     * INITIAL_CYCLE_TIME, afterwards the check runs every REGULAR_CYCLE_TIME
     * milliseconds.
     *
     * Why? In the beginning it takes a while till all components are up and
     * running. So it is likely, that in the beginning not all are immediately
     * running.
     */
    private final static int INITIAL_CYCLES = 60;
    private final static int INITIAL_CYCLE_TIME = 1_000; // in ms
    private final static int REGULAR_CYCLE_TIME = 60_000; // in ms

    private final Logger log = LoggerFactory.getLogger(OsgiValidateWorker.class);

    private final ComponentManager parent;

    public OsgiValidateWorker(ComponentManager parent) {
        this.parent = parent;
    }

    @Override
    protected void forever() {
        AtomicBoolean allConfigActivated = new AtomicBoolean(true);
        try {
            this.parent.checkForNotActivatedComponents().forEach(notActivatedCom -> {
                this.parent.logWarn(this.log, "Component [" + notActivatedCom + "] is configured but not active!");
                allConfigActivated.set(false);
            });
        } catch (Exception e) {
            this.parent.logError(this.log, e.getMessage());
            e.printStackTrace();
        }
        // cast should always work
        ((OpenemsComponent) this.parent).channel(ComponentManager.ChannelId.CONFIG_NOT_ACTIVATED).setNextValue(!allConfigActivated.get());
    }

    private int cycleCountDown = OsgiValidateWorker.INITIAL_CYCLES;

    @Override
    protected int getCycleTime() {
        if (this.cycleCountDown > 0) {
            this.cycleCountDown--;
            return OsgiValidateWorker.INITIAL_CYCLE_TIME;
        } else {
            return OsgiValidateWorker.REGULAR_CYCLE_TIME;
        }
    }

    @Override
    public void triggerNextRun() {
        // Reset Cycle-Counter on explicit run
        this.cycleCountDown = OsgiValidateWorker.INITIAL_CYCLES;
        super.triggerNextRun();
    }
}
