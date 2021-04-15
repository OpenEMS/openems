package io.openems.edge.heater.chp.kwenergy;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.heater.Heater;
import io.openems.edge.heater.HeaterState;
import io.openems.edge.heater.chp.kwenergy.api.ChpKwEnergySmartblock;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;


@Designate(ocd = Config.class, factory = true)
@Component(name = "ChpKwEnergySmartblock",
		immediate = true,
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)

/**
 * This module reads the most important variables available via Modbus from a KW Energy Smartblock CHP and maps them to
 * OpenEMS channels. The module is written to be used with the Heater interface methods.
 * When setEnableSignal() from the Heater interface is set to true with no other parameters like setPointPowerPercent()
 * specified, the CHP will turn on with default settings. The default settings are configurable in the config.
 * The CHP can be controlled with setSetPointPowerPercent() or setSetPointElectricPower().
 * setSetPointTemperature() and related methods are not supported by this CHP.
 */
public class ChpKwEnergySmartblockImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler, ChpKwEnergySmartblock {

	@Reference
	protected ConfigurationAdmin cm;

	private final Logger log = LoggerFactory.getLogger(ChpKwEnergySmartblockImpl.class);
	private boolean debug;
	private int heartbeatCounter = 0;
	private LocalDateTime connectionTimestamp;
	private int defaultSetPointPowerPercent;
	private int defaultSetPointElectricPower;
	private boolean chpError = false;
	private boolean isEnabled = false;
	private int cycleCounter = 0;

	// This is essential for Modbus to work, but the compiler does not warn you when it is missing!
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public ChpKwEnergySmartblockImpl() {
		super(OpenemsComponent.ChannelId.values(),
				ChpKwEnergySmartblock.ChannelId.values(),
				Heater.ChannelId.values());	// Even though ChpKwEnergySmartblock extends this channel, it needs to be added separately.
	}

	@Activate
	public void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbusBridgeId());
		debug = config.debug();
		connectionTimestamp = LocalDateTime.now().minusMinutes(5);	// Initialize with past time value so connection test is negative at start.
		defaultSetPointPowerPercent = config.defaultSetPointPowerPercent();
		defaultSetPointElectricPower = config.defaultSetPointElectricPower();
	}


	@Deactivate
	public void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {

		return new ModbusProtocol(this,

				// Holding register read.
				new FC3ReadRegistersTask(0, Priority.HIGH,
						// Use SignedWordElement when the number can be negative. Signed 16bit maps every number >32767
						// to negative. That means if the value you read is positive and <32767, there is no difference
						// between signed and unsigned.
						m(ChpKwEnergySmartblock.ChannelId.HR0_ERROR_BITS_1_to_16, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(16, Priority.HIGH,
						m(ChpKwEnergySmartblock.ChannelId.HR16_STATUS_BITS_1_to_16, new UnsignedWordElement(16),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(20, Priority.HIGH,
						m(ChpKwEnergySmartblock.ChannelId.HR20_STATUS_BITS_65_to_80, new UnsignedWordElement(20),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(24, Priority.HIGH,
						m(ChpKwEnergySmartblock.ChannelId.HR24_ENGINE_TEMPERATURE, new UnsignedWordElement(24),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(Heater.ChannelId.FLOW_TEMPERATURE, new UnsignedWordElement(25),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(Heater.ChannelId.RETURN_TEMPERATURE, new UnsignedWordElement(26),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(31, Priority.HIGH,
						m(ChpKwEnergySmartblock.ChannelId.HR31_ENGINE_RPM, new UnsignedWordElement(31),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)
				),
				new FC3ReadRegistersTask(34, Priority.HIGH,
						m(ChpKwEnergySmartblock.ChannelId.HR34_EFFECTIVE_ELECTRIC_POWER, new UnsignedWordElement(34),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)
				),
				new FC3ReadRegistersTask(81, Priority.HIGH,
						m(ChpKwEnergySmartblock.ChannelId.HR81_OPERATING_MODE, new UnsignedWordElement(81),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(108, Priority.HIGH,
						m(ChpKwEnergySmartblock.ChannelId.HR108_HANDSHAKE_OUT, new UnsignedWordElement(108),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChpKwEnergySmartblock.ChannelId.HR109_COMMAND_BITS_1_to_16, new UnsignedWordElement(109),
								ElementToChannelConverter.DIRECT_1_TO_1)
						// No read for SET_POINT_POWER_PERCENT and SET_POINT_POWER, since those channels immediately
						// copy setNextWrite to setNextValue.
				),
				new FC3ReadRegistersTask(112, Priority.HIGH,
						m(ChpKwEnergySmartblock.ChannelId.HR112_SET_POINT_ELECTRIC_POWER, new UnsignedWordElement(112),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)
				),
				new FC3ReadRegistersTask(119, Priority.HIGH,
						m(ChpKwEnergySmartblock.ChannelId.HR119_HANDSHAKE_IN, new UnsignedWordElement(119),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),


				// Holding register write.
				// Modbus write tasks take the "setNextWriteValue" value of a channel and send them to the device.
				// Modbus read tasks put values in the "setNextValue" field, which get automatically transferred to the
				// "value" field of the channel. By default, the "setNextWriteValue" field is NOT copied to the
				// "setNextValue" and "value" field. In essence, this makes "setNextWriteValue" and "setNextValue"/"value"
				// two separate channels.
				// That means: Modbus read tasks will not overwrite any "setNextWriteValue" values. You do not have to
				// watch the order in which you call read and write tasks.
				// Also: if you do not add a Modbus read task for a write channel, any "setNextWriteValue" values will
				// not be transferred to the "value" field of the channel, unless you add code that does that.
				new FC16WriteRegistersTask(109,
						m(ChpKwEnergySmartblock.ChannelId.HR109_COMMAND_BITS_1_to_16, new UnsignedWordElement(109),
								ElementToChannelConverter.DIRECT_1_TO_1),
						// A Modbus read commands reads everything from start address to finish address. If there is a
						// gap, you must place a dummy element to fill the gap or end the read command there and start
						// with a new read where you want to continue.
						new DummyRegisterElement(110),
						m(Heater.ChannelId.SET_POINT_POWER_PERCENT, new UnsignedWordElement(111),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(ChpKwEnergySmartblock.ChannelId.HR112_SET_POINT_ELECTRIC_POWER, new UnsignedWordElement(112),
								ElementToChannelConverter.SCALE_FACTOR_1)
				),
				new FC16WriteRegistersTask(119,
						m(ChpKwEnergySmartblock.ChannelId.HR119_HANDSHAKE_IN, new UnsignedWordElement(119),
								ElementToChannelConverter.DIRECT_1_TO_1)
				)

		);
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
			case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
				channelmapping();
				break;
		}
	}

	// Put values in channels that are not directly Modbus read values but derivatives.
	protected void channelmapping() {

		// Send and increment heartbeatCounter.
		// This code is speculative. I had no manual to explain how the handshake works, so I used the same code the
		// Buderus heater uses (where I had a manual to explain how it works). No idea if this works or not.
		try {
			setHandshakeIn(heartbeatCounter);	// Send heartbeatCounter.
		} catch (OpenemsError.OpenemsNamedException e) {
			log.warn("Couldn't write in Channel " + e.getMessage());
		}
		if (getHandshakeOut().isDefined()) {
			int receivedHeartbeatCounter = getHandshakeOut().get();	// Get last received heartbeatCounter value.
			if (receivedHeartbeatCounter == heartbeatCounter) {		// Test if the sent value was received.
				connectionTimestamp = LocalDateTime.now();			// Now we know the connection is alive. Set timestamp.
				heartbeatCounter++;
			}
		}
		boolean connectionAlive;
		if (ChronoUnit.SECONDS.between(connectionTimestamp, LocalDateTime.now()) >= 30) {	// No heart beat match for 30 seconds means connection is dead.
			connectionAlive = false;
		} else {
			connectionAlive = true;
		}
		if (heartbeatCounter >= 100) {	// Overflow protection.
			heartbeatCounter = 1;
		}

		// Decide state of enabledSignal.
		// The method isEnabledSignal() does get and reset. Calling it will clear the value (for that cycle). So you
		// need to store the value in a local variable.
		Optional<Boolean> enabledSignal = isEnabledSignal();
		if (enabledSignal.isPresent()) {
			isEnabled = enabledSignal.get();
			cycleCounter = 0;
		} else {
			// No value in the Optional.
			// Wait 5 cycles. If isEnabledSignal() has not been filled with a value again, switch to false.
			// ToDo: Ein BHKW hat begrenzte Starts, weil dauerndes ein/aus schlecht ist für den Motor. Die 5-Zyklen-kein
			//  -Signal-dann-aus-Regel kann da evtl. zu Problemen führen. Genauer: Start wird verweigert wenn schon zu
			//  oft gestartet.
			//  Ich weiß nicht was für das Smartblock BHKW gilt (hab ja kein Handbuch). Aber beim Dachs BHKW hatte ich
			//  ein Handbuch und da stand was von maximal drei Starts pro Stunde.
			//  Für ein BHKW sollte der Code träger sein. So in der Art: Wenn an, dann für mindestens 30 min an.
			if (isEnabled) {
				cycleCounter++;
				if (cycleCounter > 5) {
					isEnabled = false;
				}
			}
		}

		// Send command bits, based on settings.
		int commandBits1to16 = 0;
		if (isEnabled) {

			// Set default set point (defined in config) if the selected control mode does not have a set point specified.
			boolean noSetPoint =
					// Control mode is SetPointPowerPercent, and SET_POINT_POWER_PERCENT channel is empty.
					(((getControlModeElectricPower().isDefined() == false) || (getControlModeElectricPower().get() == false)) // No value in getControlModeElectricPower() counts as "Control mode is SetPointPowerPercent", which is the default.
							&& (getSetPointPowerPercentChannel().getNextWriteValue().isPresent() == false))
					||	// or
							// Control mode is SetPointElectricPower, and HR112_SET_POINT_ELECTRIC_POWER channel is empty.
							(((getControlModeElectricPower().isDefined()) && getControlModeElectricPower().get())
									&& (getSetPointElectricPowerChannel().getNextWriteValue().isPresent() == false));
			if (noSetPoint) {
				try {
					setSetPointElectricPower(defaultSetPointElectricPower);
					setSetPointPowerPercent(defaultSetPointPowerPercent);
				} catch (OpenemsError.OpenemsNamedException e) {
					log.warn("Couldn't write in Channel " + e.getMessage());
				}
			}

			// Command bits:
			// 0 - Steuerung über Bussystem (muss für Steuerung immer 1 sein)
			// 1 - Fehler-Quittierung (darf nur kurzzeitig angelegt werden)
			// 2 - Startanforderung (Stop bei 0)
			// 3 - Betriebsart - Startanfoderung durch Netzbezug
			// 4 - Betriebsart - Startanfoderung durch Puffer
			// 5 - Betriebsart - Regelung durch Netzbezugssollwert
			// 6 - Betriebsart - Regelung durch Gleitwert
			// 7 - Gleitwert kommt über Bus
			// 8 - Netzleistungswert kommt über Bus
			// 9 - Automatikbetrieb BHKW (Flanke)

			// Default state if no control mode is specified: use setSetPointPowerPercent().
			commandBits1to16 = 0b011000101;	// ToDo: I have no manual to look up how this is supposed to work. This is my best guess.
			if (getControlModeElectricPower().isDefined()) {
				// Choose operating mode
				if (getControlModeElectricPower().get()) {
					// True means use setSetPointElectricPower().
					commandBits1to16 = 0b0100100101;
				}
				// False means use setSetPointPowerPercent(). This is the default, so don't need to do anything.
			}
		} else {
			commandBits1to16 = 0b01;	// Turn off chp.
		}
		// At this point commandBits1to16 have been set. Now send them.
		try {
			setCommandBits1to16(commandBits1to16);
		} catch (OpenemsError.OpenemsNamedException e) {
			log.warn("Couldn't write in Channel " + e.getMessage());
		}

		// Parse error bits 1 to 16.
		int errorBits1to16 = 0;
		if (getErrorBits1to16().isDefined()) {
			errorBits1to16 = getErrorBits1to16().get();
		}
		boolean chpWarning = (errorBits1to16 & 0b01) == 0b01;
		chpError = (errorBits1to16 & 0b010) == 0b010;
		boolean chpMaintenanceNeeded = (errorBits1to16 & 0b0100) == 0b0100;
		boolean chpEmergencyShutdown = (errorBits1to16 & 0b0100000) == 0b0100000;
		boolean chpCoolantLow = (errorBits1to16 & 0b01000000) == 0b01000000;
		boolean chpLeaking = (errorBits1to16 & 0b0100000000) == 0b0100000000;

		// Parse status bits 1 to 16.
		int statusBits1to16 = 0;
		if (getStatusBits1to16().isDefined()) {
			statusBits1to16 = getStatusBits1to16().get();
		}
		boolean chpAutomaticMode = (statusBits1to16 & 0b01) == 0b01;
		boolean chpRunSignalFromModbus = (statusBits1to16 & 0b010) == 0b010;
		boolean chpManualMode = (statusBits1to16 & 0b0100) == 0b0100;
		boolean chpReadySignal = (statusBits1to16 & 0b01000) == 0b01000;
		boolean chpRunSignalRegistered = (statusBits1to16 & 0b010000) == 0b010000;
		boolean chpStartingUp = (statusBits1to16 & 0b0100000) == 0b0100000;
		boolean chpEngineRunning = (statusBits1to16 & 0b010000000) == 0b010000000;

		// Parse status bits 65 to 80.
		int statusBits65to80 = 0;
		if (getStatusBits65to80().isDefined()) {
			statusBits65to80 = getStatusBits65to80().get();
		}
		boolean chpBetriebsart0FestwertAktiv = (statusBits65to80 & 0b01000000000000) == 0b01000000000000;
		boolean chpBetriebsart1GleitwertAktiv = (statusBits65to80 & 0b010000000000000) == 0b010000000000000;
		boolean chpBetriebsart2NetzbezugAktiv = (statusBits65to80 & 0b0100000000000000) == 0b0100000000000000;

		// Set Heater interface STATUS channel
		if (connectionAlive == false) {
			setState(HeaterState.OFFLINE.name());
		} else if (chpError){
			setState(HeaterState.ERROR.name());
		} else if (chpWarning){
			setState(HeaterState.WARNING.name());
		} else if (chpEngineRunning){
			setState(HeaterState.RUNNING.name());
		} else if (chpStartingUp){
			setState(HeaterState.PREHEAT.name());
		} else if (chpReadySignal){
			setState(HeaterState.AWAIT.name());
		} else {
			// If the code gets to here, the state is undefined.
			setState(HeaterState.UNDEFINED.name());
		}

		// Build status message.
		String statusMessage = "";
		if (chpError) {
			statusMessage = statusMessage + "Störung BHKW-Anlage, ";
		}
		if (chpWarning) {
			statusMessage = statusMessage + "Warnung BHKW-Anlage, ";
		}
		if (chpMaintenanceNeeded) {
			statusMessage = statusMessage + "Wartungsaufruf BHKW, ";
		}
		if (chpEmergencyShutdown) {
			statusMessage = statusMessage + "Notabschaltung, ";
		}
		if (chpCoolantLow) {
			statusMessage = statusMessage + "Kühlwasserstand min, ";
		}
		if (chpLeaking) {
			statusMessage = statusMessage + "Leckage BHKW-Anlage, ";
		}
		if (chpAutomaticMode) {
			statusMessage = statusMessage + "Automatikbetrieb BHKW, ";
		}
		if (chpRunSignalFromModbus) {
			statusMessage = statusMessage + "Freigabe BHKW durch externes Signal, ";
		}
		if (chpManualMode) {
			statusMessage = statusMessage + "Handbetrieb BHKW, ";
		}
		if (chpReadySignal) {
			statusMessage = statusMessage + "BHKW betriebsbereit, ";
		}
		if (chpRunSignalRegistered) {
			statusMessage = statusMessage + "Anforderung steht an, ";
		}
		if (chpStartingUp) {
			statusMessage = statusMessage + "Start aktiv, ";
		}
		if (chpEngineRunning) {
			statusMessage = statusMessage + "Motor läuft, ";
		}
		if (chpBetriebsart0FestwertAktiv) {
			statusMessage = statusMessage + "Betriebsart 0 - Festwert aktiv, ";
		}
		if (chpBetriebsart1GleitwertAktiv) {
			statusMessage = statusMessage + "Betriebsart 0 - Gleitwert aktiv, ";
		}
		if (chpBetriebsart2NetzbezugAktiv) {
			statusMessage = statusMessage + "Betriebsart 0 - Netzbezug aktiv, ";
		}
		if (statusMessage.length() > 0) {
			statusMessage = statusMessage.substring(0, statusMessage.length() - 2) + ".";
		}
		_setStatusMessage(statusMessage);

		if (debug) {
			this.logInfo(this.log, "--CHP KW Energy Smartblock--");
			this.logInfo(this.log, "Engine rpm: " + getEngineRpm().get());
			this.logInfo(this.log, "Engine temperature: " + getEngineTemperature().get() + " d°C");
			this.logInfo(this.log, "Effective electrical power: " + getEffectiveElectricPower().get() + " kW");
			this.logInfo(this.log, "Flow temperature: " + getFlowTemperature() + " d°C");
			this.logInfo(this.log, "Return temperature: " + getReturnTemperature() + " d°C");
			this.logInfo(this.log, "");
			this.logInfo(this.log, "State enum: " + getCurrentState());
			this.logInfo(this.log, "Status message: " + getStatusMessage().get());
			this.logInfo(this.log, "");
		}

	}

	@Override
	public boolean setPointPowerPercentAvailable() {
		return true;
	}

	@Override
	public boolean setPointPowerAvailable() {
		// Function not supported by this CHP.
		return false;
	}

	@Override
	public boolean setPointTemperatureAvailable() {
		// Function not supported by this CHP.
		return false;
	}

	@Override
	public int calculateProvidedPower(int demand, float bufferValue) throws OpenemsError.OpenemsNamedException {
		// This function is only a rough estimate as the correlation is not linear. Output at 100% is 51 kW, at 50% it
		// is 35 kW. At least that is what the datasheet says.
		double providedThermalPower = getEffectivePowerPercent() * getMaximumThermalOutput();
		return (int)Math.round(providedThermalPower);
	}

	@Override
	public int getMaximumThermalOutput() {
		// ToDo: Validate value. Don't know right now because I have no manual for this chp. Found a datasheet online
		//  with that value, but I don't know for sure if the datasheet I found is for this chp.
		return 51;	// Unit is kW.
	}

	@Override
	public void setOffline() throws OpenemsError.OpenemsNamedException {
		setEnableSignal(false);
	}

	@Override
	public boolean hasError() {
		return chpError;
	}

	@Override
	public void requestMaximumPower() {
		// Set CHP to run at 100%, but don't set enableSignal.
		try {
			setControlModeElectricPower(false);
			setSetPointPowerPercent(100);
		} catch (OpenemsError.OpenemsNamedException e) {
			log.warn("Couldn't write in Channel " + e.getMessage());
		}
	}

	@Override
	public void setIdle() {
		// Set CHP to run at 0%, but don't switch it off.
		// ToDo: Not sure if the CHP can do that.
		try {
			setControlModeElectricPower(false);
			setSetPointPowerPercent(0);
		} catch (OpenemsError.OpenemsNamedException e) {
			log.warn("Couldn't write in Channel " + e.getMessage());
		}

	}
}
