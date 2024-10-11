package io.openems.edge.io.revpi.bsp.core;

import java.io.IOException;
import java.time.Instant;

import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.io.api.DigitalInput;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.io.api.DigitalOutput;

@Designate(ocd = Config.class, factory = false)
@Component(//
	name = "io.revpi.bsp.core", //
	immediate = true, //
	configurationPolicy = ConfigurationPolicy.REQUIRE //
) //
@EventTopics({ //
	EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
}) //
public class CoreImpl extends AbstractOpenemsComponent
	implements BoardSupportPackage, Core, DigitalOutput, DigitalInput, OpenemsComponent, EventHandler {

    private final Logger log = LoggerFactory.getLogger(CoreImpl.class);

    private final BooleanWriteChannel[] digitalOutChannels;

    private RevPiBoard board;

    private Config config;

    private Instant systemStartTime;

    @Reference
    private ComponentManager componentManager;

    @Reference
    private Sum sum;

    public CoreImpl() {
	super(OpenemsComponent.ChannelId.values(), //
		BoardSupportPackage.ChannelId.values() //
	);
	this.digitalOutChannels = new BooleanWriteChannel[] { this.getDigitalOut1WriteChannel() };
	this.systemStartTime = Instant.now();
    }

    @Activate
    void activate(ComponentContext context, Config config) {
	super.activate(context, config.id(), config.alias(), config.enabled());
	this.config = config;

	this.board = RevPiHardware.get().getBoard();
	try {
	    this.board.setA1Red();
	    this.board.setA2Red();
	} catch (IOException e) {
	    ;
	}

	this.logInfo(this.log, "activated");
    }

    @Override
    public BooleanWriteChannel[] digitalOutputChannels() {
	return this.digitalOutChannels;
    }

    @Deactivate
    protected void deactivate() {
	super.deactivate();
	this.board.close();
	this.logInfo(this.log, "deactivated");
    }

    @Override
    public void handleEvent(Event event) {
	if (!this.isEnabled()) {
	    return;
	}
	try {
	    this.updateUptime(this.systemStartTime);
	    this.updateStatusLedEdge();
	    this.updateStatusLedBackend();
	    // TODO implement LED A3
	    this.updateOnboardRelais();

	    this.getStateChannel().setNextValue(Level.OK);
	} catch (Exception e) {
	    this.getStateChannel().setNextValue(Level.FAULT);
	}
    }

    private void updateOnboardRelais() throws IOException {
	var value = this.getDigitalOut1WriteValueAndReset();
	if (value.isPresent()) {
	    var relaisState = value.get().booleanValue();
	    this.board.setRelaisState(relaisState);
	    this.setDigitalOut1Value(null);
	}
    }

    private void updateStatusLedEdge() throws IOException {

	if (this.sum.getState().isAtLeast(Level.FAULT)) {
	    this.board.setA1Red();
	    this.setStatusLedEdgeValue(LedState.RED);

	    // }else if (this.sum.getState().isAtLeast(Level.WARNING)) {
	    // this.board.setA1Oragnge();
	    // this.setStatusLedEdgeValue(LedState.ORANGE);

	} else {
	    this.board.setA1Green();
	    this.setStatusLedEdgeValue(LedState.GREEN);
	}
    }

    private void updateStatusLedBackend() throws IOException {
	boolean cloudConnected = this.getCloudConnectionState();
	if (cloudConnected) {
	    this.board.setA2Green();
	    this.setStatusLedBackendValue(LedState.GREEN);
	} else {
	    this.board.setA2Red();
	    this.setStatusLedBackendValue(LedState.RED);
	}
    }

    private boolean getCloudConnectionState() {
	try {
	    var cmp = this.componentManager.getComponent(this.config.backendComponentId());
	    return !cmp.getState().isAtLeast(Level.INFO);
	} catch (OpenemsNamedException e) {
	    return false;
	}
    }

    public RevPiBoard getRevPiBoard() {
	return this.board;
    }

    /**
     * Toggles the Hardware watchdog.
     * 
     * @throws IOException on any error
     */
    public void toggleWatchdog() throws IOException {
	this.getRevPiBoard().toggleWatchdog();
    }

    @Override
    public BooleanReadChannel[] digitalInputChannels() {
	return new BooleanReadChannel[0];
    }
}
