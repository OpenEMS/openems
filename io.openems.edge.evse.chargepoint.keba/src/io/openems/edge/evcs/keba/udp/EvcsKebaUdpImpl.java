package io.openems.edge.evcs.keba.udp;

import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.evcs.api.AbstractManagedEvcsComponent;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.DeprecatedEvcs;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.PhaseRotation;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evse.chargepoint.keba.common.EvcsKeba;
import io.openems.edge.evse.chargepoint.keba.common.Keba;
import io.openems.edge.evse.chargepoint.keba.common.KebaUdp;
import io.openems.edge.evse.chargepoint.keba.common.KebaUtils;
import io.openems.edge.evse.chargepoint.keba.udp.ReadWorker;
import io.openems.edge.evse.chargepoint.keba.udp.core.EvseChargePointKebaUdpCore;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Keba.KeContact", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		TOPIC_CYCLE_EXECUTE_WRITE, //
})
public class EvcsKebaUdpImpl extends AbstractManagedEvcsComponent implements KebaUdp, ManagedEvcs, Evcs, DeprecatedEvcs,
		ElectricityMeter, OpenemsComponent, EventHandler, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(EvcsKebaUdpImpl.class);
	private final ReadWorker readWorker;
	private final ReadHandler readHandler = new ReadHandler(this);
	private final KebaUtils kebaUtils = new KebaUtils(this);

	@Reference
	private EvcsPower evcsPower;

	@Reference(policy = STATIC, cardinality = MANDATORY)
	private EvseChargePointKebaUdpCore kebaUdpCore = null;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	protected Config config;

	private Boolean lastConnectionLostState = false;
	private InetAddress ip = null;

	public EvcsKebaUdpImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				DeprecatedEvcs.ChannelId.values(), //
				Keba.ChannelId.values(), //
				EvcsKeba.ChannelId.values(), //
				KebaUdp.ChannelId.values() //
		);
		DeprecatedEvcs.copyToDeprecatedEvcsChannels(this);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);

		// Set ReactivePower defaults
		this._setReactivePower(0);
		this._setReactivePowerL1(0);
		this._setReactivePowerL2(0);
		this._setReactivePowerL3(0);

		this.readWorker = new ReadWorker(this::send, //
				report -> {
					var receivedAMessage = this.readHandler.hasResultandReset(report);
					this.channel(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED).setNextValue(!receivedAMessage);
				});
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws UnknownHostException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.ip = InetAddress.getByName(config.ip().trim());

		this.config = config;
		this._setPowerPrecision(0.23);
		this._setChargingType(ChargingType.AC);
		this._setFixedMinimumHardwarePower(this.getConfiguredMinimumHardwarePower());
		this._setFixedMaximumHardwarePower(this.getConfiguredMaximumHardwarePower());

		/*
		 * subscribe on replies to report queries
		 */
		this.kebaUdpCore.onReceive((ip, message) -> {
			if (ip.equals(this.ip)) { // same IP -> handle message
				this.readHandler.accept(message, this.config.logVerbosity());
				this.channel(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED).setNextValue(false);
			}
		});

		// start queryWorker
		this.readWorker.activate(this.id() + "query");

	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.readWorker.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		super.handleEvent(event);
		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			this.kebaUtils.onBeforeProcessImage();
		}
		case TOPIC_CYCLE_EXECUTE_WRITE -> {
			// Clear channels if the connection to the Charging Station has been lost
			Channel<Boolean> connectionLostChannel = this.channel(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED);
			var connectionLost = connectionLostChannel.value().orElse(this.lastConnectionLostState);
			if (connectionLost != this.lastConnectionLostState) {
				if (connectionLost) {
					KebaUdp.resetChannelValues(this);
				}
				this.lastConnectionLostState = connectionLost;
			}
		}
		}
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}

	@Override
	public void triggerQuery() {
		this.readWorker.triggerNextRun();
	}

	@Override
	public String debugLog() {
		return this.kebaUtils.debugLog();
	}

	public ReadWorker getReadWorker() {
		return this.readWorker;
	}

	public ReadHandler getReadHandler() {
		return this.readHandler;
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return switch (this.config.logVerbosity()) {
		case DEBUG_LOG -> false;
		case UDP_REPORTS, WRITES -> true;
		};
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws OpenemsException {
		var phases = this.getPhasesAsInt();
		var current = Math.round((power * 1000) / phases / 230f);

		/*
		 * Limits the charging value because KEBA knows only values between 6000 and
		 * 63000
		 */
		current = Math.min(current, 63_000);

		if (current < 6000) {
			current = 0;
		}
		return this.send("currtime " + current + " 1");
	}

	@Override
	public boolean applyDisplayText(String text) throws OpenemsException {
		if (!this.config.useDisplay()) {
			return false;
		}
		return this.send(KebaUdp.preprocessDisplayTest(text));
	}

	private boolean send(String command) {
		return KebaUdp.send(this, this.ip, this.config.logVerbosity(), this.log, command);
	}

	@Override
	public boolean pauseChargeProcess() throws OpenemsException {
		return this.applyChargePowerLimit(0);
	}

	@Override
	public int getMinimumTimeTillChargingLimitTaken() {
		return 30;
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return Math.round(this.config.minHwCurrent() / 1000f) * Evcs.DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return Math.round(Evcs.DEFAULT_MAXIMUM_HARDWARE_CURRENT / 1000f) * Evcs.DEFAULT_VOLTAGE
				* Phases.THREE_PHASE.getValue();
	}

	@Override
	public boolean isReadOnly() {
		return this.config.readOnly();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Evcs.getModbusSlaveNatureTable(accessMode), //
				ManagedEvcs.getModbusSlaveNatureTable(accessMode), //
				KebaUdp.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(EvcsKebaUdpImpl.class, accessMode, 100) //
						.build());
	}
}
