package io.openems.edge.evse.chargepoint.keba.udp;

import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE;
import static io.openems.edge.evse.chargepoint.keba.udp.core.EvseChargePointKebaUdpCore.UDP_PORT;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Optional;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.PhaseRotation;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.chargepoint.keba.common.CommonConfig;
import io.openems.edge.evse.chargepoint.keba.common.EvseChargePointKeba;
import io.openems.edge.evse.chargepoint.keba.common.Utils;
import io.openems.edge.evse.chargepoint.keba.common.enums.PhaseSwitchSource;
import io.openems.edge.evse.chargepoint.keba.common.enums.PhaseSwitchState;
import io.openems.edge.evse.chargepoint.keba.common.enums.SetEnable;
import io.openems.edge.evse.chargepoint.keba.udp.core.EvseChargePointKebaUdpCore;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

// TODO implement PHASE_SWITCH_STATE

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ChargePoint.Keba.UDP", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		TOPIC_CYCLE_EXECUTE_WRITE, //
})
public class EvseChargePointKebaUdpImpl extends AbstractOpenemsComponent implements EvseChargePointKebaUdp,
		EvseChargePointKeba, EvseChargePoint, ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler {

	protected final ReadHandler readHandler = new ReadHandler(this);

	private final Logger log = LoggerFactory.getLogger(EvseChargePointKebaUdpImpl.class);
	private final Utils utils = new Utils(this);
	private final ReadWorker readWorker;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = STATIC, cardinality = MANDATORY)
	private EvseChargePointKebaUdpCore kebaUdpCore = null;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	private CommonConfig config;
	private InetAddress ip;

	public EvseChargePointKebaUdpImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvseChargePoint.ChannelId.values(), //
				EvseChargePointKeba.ChannelId.values(), //
				EvseChargePointKebaUdp.ChannelId.values() //
		);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);

		this.readWorker = new ReadWorker(this::send, //
				report -> {
					var receivedAMessage = this.readHandler.hasResultandReset(report);
					this.channel(EvseChargePointKebaUdp.ChannelId.COMMUNICATION_FAILED).setNextValue(!receivedAMessage);
					if (!receivedAMessage) {
						// Resets all channel values except the Communication_Failed channel.
						Arrays.stream(EvseChargePointKebaUdp.ChannelId.values()) //
								.filter(c -> c != EvseChargePointKebaUdp.ChannelId.COMMUNICATION_FAILED) //
								.forEach(c -> this.channel(c).setNextValue(null));
					}
				});
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws UnknownHostException, OpenemsException {
		this.config = CommonConfig.from(config);
		super.activate(context, config.id(), config.alias(), config.enabled());

		/*
		 * subscribe on replies to report queries
		 */
		this.ip = InetAddress.getByName(config.ip().trim());
		this.kebaUdpCore.onReceive((ip, message) -> {
			if (ip.equals(this.ip)) { // same IP -> handle message
				this.readHandler.accept(message, this.config.logVerbosity());
				this.channel(EvseChargePointKebaUdp.ChannelId.COMMUNICATION_FAILED).setNextValue(false);
			}
		});

		// start queryWorker
		this.readWorker.activate(this.id() + "query");
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = CommonConfig.from(config);
		super.modified(context, config.id(), config.alias(), config.enabled());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.readWorker.deactivate();
	}

	@Override
	public MeterType getMeterType() {
		return Utils.getMeterType(this.config);
	}

	@Override
	public ChargePointAbilities getChargePointAbilities() {
		return this.utils.getChargePointAbilities(this.config);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			this.utils.onBeforeProcessImage();
		}
		case TOPIC_CYCLE_EXECUTE_WRITE -> {
			if (this.config.readOnly()) {
				return;
			}
			this.setCurrent(//
					this.getSetEnableChannel().getNextWriteValueAndReset() //
							.map(ena -> OptionsEnum.getOption(SetEnable.class, ena)) //
							.orElse(SetEnable.UNDEFINED), //
					this.getSetChargingCurrentChannel().getNextWriteValueAndReset().orElse(null));
			this.setPhaseSwitch(//
					this.getSetPhaseSwitchSourceChannel().getNextWriteValueAndReset() //
							.map(pss -> OptionsEnum.getOption(PhaseSwitchSource.class, pss)) //
							.orElse(PhaseSwitchSource.UNDEFINED), //
					this.getSetPhaseSwitchStateChannel().getNextWriteValueAndReset() //
							.map(pss -> OptionsEnum.getOption(PhaseSwitchState.class, pss)) //
							.orElse(PhaseSwitchState.UNDEFINED)); //
			this.setDisplayText(Optional.empty()); // TODO
		}
		}
	}

	private void setCurrent(SetEnable setEnable, Integer setChargingCurrent) {
		final var current = switch (setEnable) {
		case DISABLE -> 0;
		case ENABLE -> setChargingCurrent;
		case UNDEFINED -> null;
		};

		if (current == null) {
			return;
		}

		this.send("currtime " + current + " 1");
	}

	private void setDisplayText(Optional<String> setText) {
		// if (!this.config.useDisplay()) {
		// return false;
		// }
		// if (text.length() > 23) {
		// text = text.substring(0, 23);
		// }
		// text = text.replace(" ", "$"); // $ == blank
		setText.ifPresent(text -> this.send("display 0 0 0 0 " + text));
	}

	private void setPhaseSwitch(PhaseSwitchSource phaseSwitchSource, PhaseSwitchState phaseSwitchState) {
		switch (phaseSwitchSource) {
		case NONE, UNDEFINED -> doNothing();
		case VIA_MODBUS, VIA_OCPP, VIA_REST, VIA_UDP //
			-> this.send("x2src " + phaseSwitchSource.getValue());
		}

		switch (phaseSwitchState) {
		case UNDEFINED -> doNothing();
		case SINGLE, THREE //
			-> this.send("x2 " + phaseSwitchState.getValue());
		}
	}

	/**
	 * Send UDP message to KEBA KeContact. Returns true if sent successfully
	 *
	 * @param command Command to send
	 * @return true if sent
	 */
	protected boolean send(String command) {
		var raw = command.getBytes();
		var packet = new DatagramPacket(raw, raw.length, this.ip, UDP_PORT);
		try (DatagramSocket datagrammSocket = new DatagramSocket()) {
			datagrammSocket.send(packet);

			switch (this.config.logVerbosity()) {
			case DEBUG_LOG -> doNothing();
			case WRITES, UDP_REPORTS -> this.logInfo(this.log, //
					"Sent [" + command + "] successfully");
			}
			return true;
		} catch (SocketException e) {
			this.logError(this.log, "Unable to open UDP socket for sending [" + command + "] to ["
					+ this.ip.getHostAddress() + "]: " + e.getMessage());
		} catch (IOException e) {
			this.logError(this.log, "Unable to send [" + command + "] UDP message to [" + this.ip.getHostAddress()
					+ "]: " + e.getMessage());
		}
		return false;
	}

	/**
	 * Triggers an immediate execution of query reports.
	 */
	protected void triggerQuery() {
		this.readWorker.triggerNextRun();
	}

	@Override
	public String debugLog() {
		return this.utils.debugLog(this.config);
	}

	@Override
	public void apply(ChargePointActions actions) {
		this.utils.handleChargePointActions(this.config, actions);
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public PhaseSwitchSource getRequiredPhaseSwitchSource() {
		return PhaseSwitchSource.VIA_UDP;
	}
}
