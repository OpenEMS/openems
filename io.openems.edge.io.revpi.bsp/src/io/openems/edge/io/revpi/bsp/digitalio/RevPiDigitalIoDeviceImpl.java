package io.openems.edge.io.revpi.bsp.digitalio;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.StringTokenizer;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.io.revpi.bsp.core.RevPiHardware;
import io.openems.edge.io.revpi.bsp.core.RevPiHardware.RevPiDigitalIo;

@Designate(ocd = Config.class, factory = true)
@Component(//
	name = "io.revpi.bsp.digitalio", //
	immediate = true, //
	configurationPolicy = ConfigurationPolicy.REQUIRE, property = { //
		EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
	})
public class RevPiDigitalIoDeviceImpl extends AbstractOpenemsComponent
	implements DigitalOutput, DigitalInput, OpenemsComponent, EventHandler {

    private static final Object INVALIDATE_CHANNEL = null;
    private static int MAX_GPIO_DIO = 14;
    private static int MAX_GPIO_INPUTS_DI = 16;
    private static int MAX_GPIO_OUTPUTS_DO = 16;

    private final Logger log = LoggerFactory.getLogger(RevPiDigitalIoDeviceImpl.class);
    private final BooleanWriteChannel[] channelOut;
    private final BooleanReadChannel[] channelIn;
    private final BooleanReadChannel[] channelOutDbg;

    private Config config = null;

    private RevPiDigitalIo revPiDigitalIo;

    private boolean[] simInData;

    /**
     * Note: On RevPi DIO only 14 are used, on RevPi DI and DO 16 channels are used.
     */
    private Boolean[] hardwareOutputState = new Boolean[MAX_GPIO_OUTPUTS_DO];

    public RevPiDigitalIoDeviceImpl() {
	super(//
		OpenemsComponent.ChannelId.values(), //
		DigitalOutput.ChannelId.values(), //
		DigitalInput.ChannelId.values(), //
		RevPiDioChannelId.values() //
	);
	this.channelOut = new BooleanWriteChannel[] { //
		this.channel(RevPiDioChannelId.OUT_1), //
		this.channel(RevPiDioChannelId.OUT_2), //
		this.channel(RevPiDioChannelId.OUT_3), //
		this.channel(RevPiDioChannelId.OUT_4), //
		this.channel(RevPiDioChannelId.OUT_5), //
		this.channel(RevPiDioChannelId.OUT_6), //
		this.channel(RevPiDioChannelId.OUT_7), //
		this.channel(RevPiDioChannelId.OUT_8), //
		this.channel(RevPiDioChannelId.OUT_9), //
		this.channel(RevPiDioChannelId.OUT_10), //
		this.channel(RevPiDioChannelId.OUT_11), //
		this.channel(RevPiDioChannelId.OUT_12), //
		this.channel(RevPiDioChannelId.OUT_13), //
		this.channel(RevPiDioChannelId.OUT_14), //
		this.channel(RevPiDioChannelId.OUT_15), //
		this.channel(RevPiDioChannelId.OUT_16) //
	};

	this.channelOutDbg = new BooleanReadChannel[] { //
		this.channel(RevPiDioChannelId.DEBUG_OUT1), //
		this.channel(RevPiDioChannelId.DEBUG_OUT2), //
		this.channel(RevPiDioChannelId.DEBUG_OUT3), //
		this.channel(RevPiDioChannelId.DEBUG_OUT4), //
		this.channel(RevPiDioChannelId.DEBUG_OUT5), //
		this.channel(RevPiDioChannelId.DEBUG_OUT6), //
		this.channel(RevPiDioChannelId.DEBUG_OUT7), //
		this.channel(RevPiDioChannelId.DEBUG_OUT8), //
		this.channel(RevPiDioChannelId.DEBUG_OUT9), //
		this.channel(RevPiDioChannelId.DEBUG_OUT10), //
		this.channel(RevPiDioChannelId.DEBUG_OUT11), //
		this.channel(RevPiDioChannelId.DEBUG_OUT12), //
		this.channel(RevPiDioChannelId.DEBUG_OUT13), //
		this.channel(RevPiDioChannelId.DEBUG_OUT14), //
		this.channel(RevPiDioChannelId.DEBUG_OUT15), //
		this.channel(RevPiDioChannelId.DEBUG_OUT16) //
	};

	this.channelIn = new BooleanReadChannel[] { //
		this.channel(RevPiDioChannelId.IN_1), //
		this.channel(RevPiDioChannelId.IN_2), //
		this.channel(RevPiDioChannelId.IN_3), //
		this.channel(RevPiDioChannelId.IN_4), //
		this.channel(RevPiDioChannelId.IN_5), //
		this.channel(RevPiDioChannelId.IN_6), //
		this.channel(RevPiDioChannelId.IN_7), //
		this.channel(RevPiDioChannelId.IN_8), //
		this.channel(RevPiDioChannelId.IN_9), //
		this.channel(RevPiDioChannelId.IN_10), //
		this.channel(RevPiDioChannelId.IN_11), //
		this.channel(RevPiDioChannelId.IN_12), //
		this.channel(RevPiDioChannelId.IN_13), //
		this.channel(RevPiDioChannelId.IN_14), //
		this.channel(RevPiDioChannelId.IN_15), //
		this.channel(RevPiDioChannelId.IN_16) //
	};

    }

    private void setAllOutput(boolean setOn) {
	for (BooleanWriteChannel ch : this.channelOut) {
	    try {
		ch.setNextWriteValue(setOn);
	    } catch (OpenemsNamedException e) {
		; // ignore
	    }
	}
    }

    private void updateDataOutFromHardware() {
	if (this.config.isSimulationMode()) {
	    return;
	}

	// read all digital out pins also, because someone may have already set this pin
	// from outside of openems
	var gpioInputsCnt = MAX_GPIO_INPUTS_DI;
	if (this.config.revpiType().typeSelector == ExpansionModule.REVPI_DIO.typeSelector) {
	    gpioInputsCnt = MAX_GPIO_DIO;
	}
	for (var idx = 0; idx < gpioInputsCnt; idx++) {
	    try {
		var in = this.revPiDigitalIo.getDigital(this._getRevPiChannelAlias(true, idx));

		if (this.hardwareOutputState[idx] == null) {
		    this.hardwareOutputState[idx] = in;
		    this.channelOut[idx].setNextWriteValue(in);
		} else {
		    if (this.hardwareOutputState[idx].booleanValue() != in) {
			this.hardwareOutputState[idx] = in;
			this.channelOut[idx].setNextWriteValue(in);
		    }
		}

	    } catch (Exception e) {
		this.logError(this.log, "Unable to update channel values ex: " + e.getMessage());
		this.channelOut[idx].setNextValue(INVALIDATE_CHANNEL);
	    }
	}
    }

    private void updateDataInChannels() {

	// read all digital in pins
	var gpioInputsCnt = MAX_GPIO_INPUTS_DI;
	if (this.config.revpiType().typeSelector == ExpansionModule.REVPI_DIO.typeSelector) {
	    gpioInputsCnt = MAX_GPIO_DIO;
	}

	for (var i = 0; i < gpioInputsCnt; i++) {
	    try {
		var in = this.getData(i);
		var inOpt = Optional.ofNullable(in);
		if (inOpt.isPresent()) {
		    this.channelIn[i].setNextValue(in);
		} else {
		    this.channelIn[i].setNextValue(INVALIDATE_CHANNEL);
		}

	    } catch (Exception e) {
		this.channel(RevPiDioChannelId.LAST_INVALIDATED_CHANNEL).setNextValue(i);
		this.logError(this.log, "Unable to update channel value for idx: " + i + " ex: " + e.getMessage());
		this.channelIn[i].setNextValue(INVALIDATE_CHANNEL);
	    }
	}
    }

    /**
     * NOTE data out will only be set once if the channel value changes.
     */
    private void updateDataOutChannels() {

	var gpioOutputsCnt = MAX_GPIO_OUTPUTS_DO;
	if (this.config.revpiType().typeSelector == ExpansionModule.REVPI_DIO.typeSelector) {
	    gpioOutputsCnt = MAX_GPIO_DIO;
	} else if (this.config.revpiType().typeSelector == ExpansionModule.REVPI_DI.typeSelector) {
	    return;
	}

	// write new state to digital out pins
	for (var idx = 0; idx < gpioOutputsCnt; idx++) {
	    try {
		var readValue = this.channelOut[idx].value().asOptional();
		var writeValue = this.channelOut[idx].getNextWriteValueAndReset();
		if (!writeValue.isPresent()) {
		    // no write value
		    continue;
		}
		if (Objects.equals(readValue, writeValue)) {
		    // read value = write value
		    continue;
		}

		if (this.revPiDigitalIo != null) {
		    this.revPiDigitalIo.setDigital(this._getRevPiChannelAlias(true, idx), writeValue.get());
		}
		this.logInfo(this.log, this.channelOut[idx].channelId() + " " + writeValue.get());
		this.channelOut[idx].setNextValue(writeValue.get());

	    } catch (Exception e) {
		this.logError(this.log, "Unable to update channel out values ex: " + e.getMessage());
	    }
	}

    }

    private String _getRevPiChannelAlias(boolean isOutput, int idx) throws OpenemsException {
	if (isOutput) {
	    return this.config.out()[idx];
	}
	return this.config.in()[idx];
    }

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsException {
	super.activate(context, config.id(), config.alias(), config.enabled());
	this.config = config;
	this.channel(RevPiDioChannelId.LAST_INVALIDATED_CHANNEL).setNextValue(-1);

	if (this.config.isSimulationMode()) {
	    return;
	}
	try {
	    this.validateConfig();
	    this.revPiDigitalIo = RevPiHardware.get().getDigitalIo();
	    if (this.config.updateOutputFromHardware()) {
		this.updateDataOutFromHardware();
	    } else {
		this.setAllOutput(false);
	    }
	} catch (Exception e) {
	    this.logError(this.log, "Unable to activate ex: " + e.getMessage());
	    throw new OpenemsException("Unable to activate ex: " + e.getMessage());
	}
    }

    private void validateConfig() throws OpenemsException {
	if (this.config.revpiType().typeSelector == ExpansionModule.REVPI_DIO.typeSelector) {
	    if (this.config.in().length != MAX_GPIO_DIO) {
		throw new OpenemsException("REVPI DIO must have " + MAX_GPIO_DIO + " IN aliases");
	    }
	    if (this.config.out().length != MAX_GPIO_DIO) {
		throw new OpenemsException("REVPI DIO must have " + MAX_GPIO_DIO + " OUT aliases");
	    }
	} else if (this.config.revpiType().typeSelector == ExpansionModule.REVPI_DI.typeSelector) {
	    if (this.config.in().length != MAX_GPIO_INPUTS_DI) {
		throw new OpenemsException("REVPI DI must have " + MAX_GPIO_INPUTS_DI + " IN aliases");

	    }
	    if (this.config.out().length > 1) {
		throw new OpenemsException("REVPI DI must have 0 OUT aliases");
	    }
	    if (this.config.updateOutputFromHardware() == true) {
		throw new OpenemsException("REVPI DI must have 'Read Output' set to false");
	    }
	} else if (this.config.revpiType().typeSelector == ExpansionModule.REVPI_DO.typeSelector) {
	    if (this.config.out().length != MAX_GPIO_OUTPUTS_DO) {
		throw new OpenemsException("REVPI DO must have " + MAX_GPIO_OUTPUTS_DO + " OUT aliases");
	    }
	    if (this.config.in().length != 0) {
		throw new OpenemsException("REVPI DO must have 0 IN aliases");
	    }
	}

    }

    @Deactivate
    protected void deactivate() {
	this.setAllOutput(false);
	super.deactivate();
	try {
	    if (!this.config.isSimulationMode()) {
		this.revPiDigitalIo.close();
	    }
	} catch (IOException e) {
	    this.logError(this.log, "Exception on closing driver ex: " + e.getMessage());
	    e.printStackTrace();
	}
	this.revPiDigitalIo = null;
    }

    @Override
    public void handleEvent(Event event) {
	if (!this.isEnabled()) {
	    return;
	}
	switch (event.getTopic()) {
	case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
	    this.eventBeforeProcessImage();
	    break;

	case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
	    this.eventExecuteWrite();
	    break;
	}
    }

    /**
     * Execute on Cycle Event "Before Process Image".
     */
    private void eventBeforeProcessImage() {
	this.updateDataInChannels();
	if (this.config.updateOutputFromHardware()) {
	    this.updateDataOutFromHardware();
	}
    }

    /**
     * Execute on Cycle Event "Execute Write".
     */
    private void eventExecuteWrite() {
	this.updateDataOutChannels();
    }

    /**
     * Reads the data either from the given DATA IN hardware port or from the
     * simulation data.
     * 
     * @param idx the index
     * @return true, if the data could be read
     */
    private boolean getData(int idx) throws Exception {
	var in = false;
	if (this.config.isSimulationMode()) {
	    if (this.simInData == null) {
		var sd = new boolean[this.channelIn.length];
		var cnt = 0;
		var st = new StringTokenizer(this.config.simulationDataIn());
		while (st.hasMoreTokens()) {
		    sd[cnt++] = (Integer.parseInt(st.nextToken()) == 1 ? true : false);
		}
		this.simInData = sd;
	    }
	    return this.simInData[idx];
	} else {
	    in = this.revPiDigitalIo.getDigital(this._getRevPiChannelAlias(false, idx));
	}
	return in;
    }

    private void appendBool(StringBuilder b, Optional<Boolean> val) {
	if (val.isPresent()) {
	    if (val.get()) {
		b.append("1");
	    } else {
		b.append("0");
	    }
	} else {
	    b.append("-");
	}
    }

    @Override
    public String debugLog() {
	var b = new StringBuilder();
	var i = 0;

	if (this.config.revpiType().typeSelector != ExpansionModule.REVPI_DO.typeSelector) {
	    b.append("IN:");
	    for (var channel : this.channelIn) {
		var valueOpt = channel.value().asOptional();
		this.appendBool(b, valueOpt);
		if ((i++) % 4 == 3) {
		    b.append(" ");
		}
	    }
	}
	i = 0;

	if (this.config.revpiType().typeSelector != ExpansionModule.REVPI_DI.typeSelector) {
	    b.append("  OUT:");
	    this.channel(RevPiDioChannelId.DEBUG_OUT1);

	    for (var channel : this.channelOutDbg) {
		var valueOpt = channel.value().asOptional();
		this.appendBool(b, valueOpt);
		if ((i++) % 4 == 3) {
		    b.append(" ");
		}
	    }
	}
	return b.toString();
    }

    @Override
    public BooleanReadChannel[] digitalInputChannels() {
	return this.channelIn;
    }

    @Override
    public BooleanWriteChannel[] digitalOutputChannels() {
	return this.channelOut;
    }

}
