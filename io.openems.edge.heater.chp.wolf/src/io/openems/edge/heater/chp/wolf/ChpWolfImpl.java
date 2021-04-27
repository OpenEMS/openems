package io.openems.edge.heater.chp.wolf;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.heater.api.ChpBasic;
import io.openems.edge.heater.chp.wolf.api.ChpWolfChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;

import java.util.Optional;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Chp.Wolf",
		immediate = true,
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)

/**
 * This module reads all variables available via Modbus from a Wolf CHP and maps them to OpenEMS
 * channels. WriteChannels can be used to send commands to the CHP via "setNextWriteValue" method.
 */
public class ChpWolfImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler, ChpWolfChannel {

	private final Logger log = LoggerFactory.getLogger(ChpWolfImpl.class);
	private int testcounter = 0;
	private boolean debug;
	private int commandCycler = 0;

	@Reference
	protected ConfigurationAdmin cm;

	// This is essential for Modbus to work, but the compiler does not warn you when it is missing!
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public ChpWolfImpl() {
		super(OpenemsComponent.ChannelId.values(),
				ChpWolfChannel.ChannelId.values(),
				ChpBasic.ChannelId.values());	// Even though ChpWolfChannel extends this channel, it needs to be added separately.
	}


	@Activate
	public void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id());
		debug = config.debug();
	}


	@Deactivate
	public void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {

		return new ModbusProtocol(this,

				new FC3ReadRegistersTask(2, Priority.HIGH,
						m(ChpWolfChannel.ChannelId.HR2_STATUS_BITS1, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(11, Priority.HIGH,
						m(ChpWolfChannel.ChannelId.HR11_STATUS_BITS2, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(27, Priority.LOW,
						m(ChpBasic.ChannelId.FLOW_TEMPERATURE, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChpBasic.ChannelId.RETURN_TEMPERATURE, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(32, Priority.LOW,
						m(ChpWolfChannel.ChannelId.HR32_BUFFERTANK_TEMP_UPPER, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChpWolfChannel.ChannelId.HR33_BUFFERTANK_TEMP_MIDDLE, new UnsignedWordElement(1),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChpWolfChannel.ChannelId.HR34_BUFFERTANK_TEMP_LOWER, new UnsignedWordElement(2),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(263, Priority.HIGH,
						m(ChpBasic.ChannelId.EFFECTIVE_ELECTRIC_POWER, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(314, Priority.HIGH,
						m(ChpWolfChannel.ChannelId.HR314_RPM, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(3588, Priority.HIGH,
						m(ChpWolfChannel.ChannelId.HR3588_RUNTIME, new UnsignedDoublewordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChpWolfChannel.ChannelId.HR3590_ENGINE_STARTS, new UnsignedWordElement(2),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(3596, Priority.LOW,
						m(ChpWolfChannel.ChannelId.HR3596_ELECTRICAL_WORK, new UnsignedDoublewordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(6358, Priority.LOW,
						m(ChpWolfChannel.ChannelId.ELECTRIC_POWER_SETPOINT, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChpWolfChannel.ChannelId.EINSPEISEMANAGEMENT_SETPOINT, new UnsignedWordElement(1),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChpWolfChannel.ChannelId.RESERVE_SETPOINT, new UnsignedWordElement(2),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChpBasic.ChannelId.ENABLE_SIGNAL, new UnsignedWordElement(3),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),

				new FC16WriteRegistersTask(6358,
						m(ChpWolfChannel.ChannelId.HR6358_WRITE_BITS1, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChpWolfChannel.ChannelId.HR6359_WRITE_BITS2, new UnsignedWordElement(1),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChpWolfChannel.ChannelId.HR6360_WRITE_BITS3, new UnsignedWordElement(2),
								ElementToChannelConverter.DIRECT_1_TO_1)
				)
		);
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
			case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
				//channeltest();	// Just for testing
				channelmapping();
				break;
		}
	}

	// Put values in channels that are not directly Modbus read values but derivatives.
	protected void channelmapping() {

		// The value in the channel can be null. Use "orElse" to avoid null pointer exception.
		int statusBits40003 = getStatusBits40003().orElse(0);
		int statusBits40012 = getStatusBits40012().orElse(0);
		String warnMessage = "";

        if ((statusBits40003 & 32) == 32) {
            warnMessage = warnMessage + "Kühlwasserdruck minimum Gasmotor, ";
        }
        if ((statusBits40003 & 64) == 64) {
            warnMessage = warnMessage + "Tank Ölvorlage auf Minimum, ";
        }
        if ((statusBits40003 & 128) == 128) {
            warnMessage = warnMessage + "Schmieröldruck Gasmotor minimum, ";
        }
        if ((statusBits40003 & 256) == 256) {
            warnMessage = warnMessage + "Generatorschutz ausgelöst, ";
        }
        if ((statusBits40003 & 512) == 512) {
            warnMessage = warnMessage + "NA-Schutz Extern ausgelöst, ";
        }
        if ((statusBits40003 & 1024) == 1024) {
            warnMessage = warnMessage + "Erdgasdruck an der Gasregelstrecke zu gering, ";
        }
        if ((statusBits40003 & 2048) == 0) {    // Handbuch sagt hier 0=ein, also vermutlich Störung ist wenn hier 0 ist.
            warnMessage = warnMessage + "Durchflußwächter Kühlwasserkreis Gasmotor misst keinen Fluß, ";
        }
        if ((statusBits40003 & 4096) == 4096) {
            warnMessage = warnMessage + "Sicherheitstempaturbegrenzer Gasmotor geschaltet, ";
        }
        if ((statusBits40003 & 8192) == 8192) {
            warnMessage = warnMessage + "Motorschutz Pumpe Kühlwasser ausgelöst, ";
        }
        if ((statusBits40003 & 16384) == 16384) {
            warnMessage = warnMessage + "Motorschutz Pumpe Heizung zum Verteiler ausgelöst, ";
        }
        if ((statusBits40003 & 32768) == 32768) {
            warnMessage = warnMessage + "Sicherungsautomat Lüfter Schallhaube ausgelöst, ";
        }
        if ((statusBits40012 & 8192) == 8192) {
            warnMessage = warnMessage + "Meldung Wartungintervall erreicht, ";
        }
        if (warnMessage.length() > 0) {
            warnMessage = warnMessage.substring(0, warnMessage.length() - 2) + ".";
            this.channel(ChpBasic.ChannelId.WARNING).setNextValue(true);
        } else {
            this.channel(ChpBasic.ChannelId.WARNING).setNextValue(false);
        }
        this.channel(ChpWolfChannel.ChannelId.WARNING_MESSAGE).setNextValue(warnMessage);

        if ((statusBits40012 & 512) == 512) {
            this.channel(ChpBasic.ChannelId.READY).setNextValue(true);
        } else {
            this.channel(ChpBasic.ChannelId.READY).setNextValue(false);
        }
		if ((statusBits40012 & 2048) == 2048) {
			this.channel(ChpBasic.ChannelId.ERROR).setNextValue(true);
		} else {
			this.channel(ChpBasic.ChannelId.ERROR).setNextValue(false);
		}
		
		
		// Parse on/off by checking engine rpm.
		if (getRpm().isDefined()) {
			if (getRpm().get() > 10) {
				_setEnableSignal(true);
			} else {
				_setEnableSignal(false);
			}
		}
			
		
		// Write bits mapping.
		// This chp has an unusual way of handling write commands. Instead of mapping one command to one 
		// register, four commands are mapped to three registers that need to be set simultaneously.
		// HR6358 - always 2.
		// HR6359 - the value you want to write.
		// HR6360 - code deciding which command it is.
		//			35 = Sollwert elektrische Leistung in kW
		//			36 = Sollwert Einspeisemanagement (optional)
		//			37 = Reserve
		//			38 = on/off, send 1 for on, 0 for off.
		// 
		// You can only send one command per cycle (need to write all three registers for one command)
		commandCycler++;
		switch (commandCycler) {
			case 1:
				Optional<Boolean> enabledSignal = getEnableSignalChannel().getNextWriteValueAndReset();
				if (enabledSignal.isPresent()) {
					int writeValue = 0;
					if (enabledSignal.get()) {
						writeValue = 1;
					}
					try {
						setWriteBits1(2);
						setWriteBits2(writeValue);
						setWriteBits1(38);
					} catch (OpenemsNamedException e) {
						this.logError(this.log, "Error setting next write value: " + e);
					}
				}
				break;
			case 2:
				Optional<Integer> electricPowerSetpoint = getElectricPowerSetpointChannel().getNextWriteValueAndReset();
				if (electricPowerSetpoint.isPresent()) {
					int writeValue = electricPowerSetpoint.get();
					// Update channel.
					getElectricPowerSetpointChannel().setNextValue(writeValue);
					try {
						setWriteBits1(2);
						setWriteBits2(writeValue);
						setWriteBits1(35);
					} catch (OpenemsNamedException e) {
						this.logError(this.log, "Error setting next write value: " + e);
					}
				}
				break;
			case 3:
				Optional<Integer> einspeisemenagement = getEinspeisemanagementSetpointChannel().getNextWriteValueAndReset();
				if (einspeisemenagement.isPresent()) {
					int writeValue = einspeisemenagement.get();
					// Update channel.
					getEinspeisemanagementSetpointChannel().setNextValue(writeValue);
					try {
						setWriteBits1(2);
						setWriteBits2(writeValue);
						setWriteBits1(36);
					} catch (OpenemsNamedException e) {
						this.logError(this.log, "Error setting next write value: " + e);
					}
				}
				break;
			default:
				commandCycler = 0;
				Optional<Integer> reserve = getReserveSetpointChannel().getNextWriteValueAndReset();
				if (reserve.isPresent()) {
					int writeValue = reserve.get();
					// Update channel.
					getReserveSetpointChannel().setNextValue(writeValue);
					try {
						setWriteBits1(2);
						setWriteBits2(writeValue);
						setWriteBits1(37);
					} catch (OpenemsNamedException e) {
						this.logError(this.log, "Error setting next write value: " + e);
					}
				}
				
		}

		

		if (debug) {
			this.logInfo(this.log, "--Status Bits 40003--");
			this.logInfo(this.log, "0 - RM Ge.Schalter - Rückmeldung Generatorschalter geschlossen = " + (((statusBits40003 & 1) == 1) ? 1 : 0));
			this.logInfo(this.log, "1 - RM Ne.Schalter - Rückmeldung Netzparallelbetrieb möglich = " + (((statusBits40003 & 2) == 2) ? 1 : 0));
			this.logInfo(this.log, "2 - Fernstart - Start-Stopp Eingang = " + (((statusBits40003 & 4) == 4) ? 1 : 0));
			this.logInfo(this.log, "3 - Not stop - Meldung Not-Aus gedrückt = " + (((statusBits40003 & 8) == 8) ? 1 : 0));
			this.logInfo(this.log, "4 - Stellung Auto - Automatik Fernstart möglich = " + (((statusBits40003 & 16) == 16) ? 1 : 0));
			this.logInfo(this.log, "5 - Wasserdruck - Kühlwasserdruck minimum Gasmotor = " + (((statusBits40003 & 32) == 32) ? 1 : 0));
			this.logInfo(this.log, "6 - Ölvorlage - Tank Ölvorlage auf Minimum = " + (((statusBits40003 & 64) == 64) ? 1 : 0));
			this.logInfo(this.log, "7 - Oeldruck min - Schmieröldruck Gasmotor minimum = " + (((statusBits40003 & 128) == 128) ? 1 : 0));
			this.logInfo(this.log, "8 - Gen.Schutz - Generatorschutz ausgelöst = " + (((statusBits40003 & 256) == 256) ? 1 : 0));
			this.logInfo(this.log, "9 - NA-Schutz Extern - NA-Schutz Extern ausgelöst = " + (((statusBits40003 & 512) == 512) ? 1 : 0));
			this.logInfo(this.log, "10 - Erdgasdruck min - Erdgasdruck an der Gasregelstrecke zu gering = " + (((statusBits40003 & 1024) == 1024) ? 1 : 0));
			this.logInfo(this.log, "11 - Durchfluß - Durchflußwächter Kühlwasserkreis Gasmotor geschaltet = " + (((statusBits40003 & 2048) == 2048) ? 1 : 0));
			this.logInfo(this.log, "12 - STB Motor - Sicherheitstempaturbegrenzer Gasmotor geschaltet = " + (((statusBits40003 & 4096) == 4096) ? 1 : 0));
			this.logInfo(this.log, "13 - Stör P Motor - Motorschutz Pumpe Kühlwasser ausgelöst = " + (((statusBits40003 & 8192) == 8192) ? 1 : 0));
			this.logInfo(this.log, "14 - Stör P Heizung - Motorschutz Pumpe Heizung zum Verteiler ausgelöst = " + (((statusBits40003 & 16384) == 16384) ? 1 : 0));
			this.logInfo(this.log, "15 - Stör Lüfter - Sicherungsautomat Lüfter Schallhaube ausgelöst = " + (((statusBits40003 & 32768) == 32768) ? 1 : 0));
			this.logInfo(this.log, "");
			this.logInfo(this.log, "--Status Bits 40012--");
			this.logInfo(this.log, "0 - Starter - Anlasser eingeschaltet = " + (((statusBits40012 & 1) == 1) ? 1 : 0));
			this.logInfo(this.log, "1 - Kessel - Freigabe Kessel = " + (((statusBits40012 & 2) == 2) ? 1 : 0));
			this.logInfo(this.log, "2 - Gasventile - Freigabe Gasventile = " + (((statusBits40012 & 4) == 4) ? 1 : 0));
			this.logInfo(this.log, "3 - Gasventile - Freigabe Gasventile = " + (((statusBits40012 & 8) == 8) ? 1 : 0));
			this.logInfo(this.log, "4 - GLS Aus/Ein - Generatorschalter Ein / AUS = " + (((statusBits40012 & 16) == 16) ? 1 : 0));
			this.logInfo(this.log, "5 - Speicher - Freigabe Speicherentladepumpe = " + (((statusBits40012 & 32) == 32) ? 1 : 0));
			this.logInfo(this.log, "6 - Pumpen+Lüfter - Pumpe Motor Ein, Heizung Ein, Ladeluft Ein, Lüfter Schallhaube Ein = " + (((statusBits40012 & 64) == 64) ? 1 : 0));
			this.logInfo(this.log, "7 - Zuendung - Freigabe Zündung = " + (((statusBits40012 & 128) == 128) ? 1 : 0));
			this.logInfo(this.log, "8 - Reserve = " + (((statusBits40012 & 256) == 256) ? 1 : 0));
			this.logInfo(this.log, "9 - Bereit - Meldung Bereit = " + (((statusBits40012 & 512) == 512) ? 1 : 0));
			this.logInfo(this.log, "10 - Umluft - Anforderung Umluftklappe öffnen = " + (((statusBits40012 & 1024) == 1024) ? 1 : 0));
			this.logInfo(this.log, "11 - Störung = " + (((statusBits40012 & 2048) == 2048) ? 1 : 0));
			this.logInfo(this.log, "12 - Pumpe Ölvorlage - Anforderung Pumpe Ölvorlage = " + (((statusBits40012 & 4096) == 4096) ? 1 : 0));
			this.logInfo(this.log, "13 - Service Zeit - Meldung Wartungintervall erreicht = " + (((statusBits40012 & 8192) == 8192) ? 1 : 0));
			this.logInfo(this.log, "14 - res. = " + (((statusBits40012 & 16384) == 16384) ? 1 : 0));
			this.logInfo(this.log, "15 - res. = " + (((statusBits40012 & 32768) == 32768) ? 1 : 0));
			this.logInfo(this.log, "");
			this.logInfo(this.log, "Vorlauf Temperatur: " + getFlowTemperature());
			this.logInfo(this.log, "Rücklauf Temperatur: " + getReturnTemperature());
			this.logInfo(this.log, "Pufferspeicher Temperatur oben: " + (getBufferTankTempUpper().orElse(0) / 10.0) + "°C");
			this.logInfo(this.log, "Pufferspeicher Temperatur mitte: " + (getBufferTankTempMiddle().orElse(0) / 10.0) + "°C");
			this.logInfo(this.log, "Pufferspeicher Temperatur unten: " + (getBufferTankTempLower().orElse(0) / 10.0) + "°C");
			this.logInfo(this.log, "Elektrische Leistung: " + getEffectiveElectricPower());
			this.logInfo(this.log, "Motor Drehzeahl: " + getRpm().get() + " rpm");
			this.logInfo(this.log, "Laufzeit: " + getRuntime().get() + " h");
			this.logInfo(this.log, "Anzahl der Starts: " + getEngineStarts().get());
			this.logInfo(this.log, "Erzeugte elektrische Arbeit gesamt: " + getElectricalWork().get() + " kWh");
			this.logInfo(this.log, "");
			this.logInfo(this.log, "--Schreibbare Parameter--");
			this.logInfo(this.log, "Sollwert elektrische Leistung in kW: " + getElectricPowerSetpointChannel().value().get());
			this.logInfo(this.log, "Sollwert Einspeisemanagement: " + getEinspeisemanagementSetpointChannel().value().get());
			this.logInfo(this.log, "Reserve: " + getReserveSetpointChannel().value().get());
			this.logInfo(this.log, "On / Off: " + getEnableSignal());
			this.logInfo(this.log, "");
		}

	}

	// Just for testing. Also, example code with some explanations.
	protected void channeltest() {

		if (testcounter == 5) {

		}

		testcounter++;
	}
}
