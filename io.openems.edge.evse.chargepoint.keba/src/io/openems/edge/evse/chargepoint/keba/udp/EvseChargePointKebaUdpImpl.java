package io.openems.edge.evse.chargepoint.keba.udp;

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
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.PhaseRotation;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.chargepoint.keba.common.CommonConfig;
import io.openems.edge.evse.chargepoint.keba.common.EvseChargePointKeba;
import io.openems.edge.evse.chargepoint.keba.common.Utils;
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
public class EvseChargePointKebaUdpImpl extends AbstractOpenemsComponent implements EvseChargePointKeba,
		EvseChargePoint, ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler {

	protected final ReadHandler readHandler = new ReadHandler(this);

	private final Logger log = LoggerFactory.getLogger(EvseChargePointKebaUdpImpl.class);
	private final Utils utils = new Utils(this);
	private final ReadWorker readWorker = new ReadWorker(this);

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
				this.readHandler.accept(message);
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
	public ChargeParams getChargeParams() {
		return this.utils.getChargeParams(this.config);
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
			this.setCurrent(this.getSetEnableChannel().getNextWriteValueAndReset(),
					this.getSetChargingCurrentChannel().getNextWriteValueAndReset());
			this.setDisplayText(Optional.empty()); // TODO
		}
		}
	}

	private void setCurrent(Optional<Integer> setEnable, Optional<Integer> setChargingCurrent) {
		setChargingCurrent.ifPresent(current -> this.send("currtime " + current + " 1"));
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

	/**
	 * Send UDP message to KEBA KeContact. Returns true if sent successfully
	 *
	 * @param s Message to send
	 * @return true if sent
	 */
	protected boolean send(String s) {
		var raw = s.getBytes();
		var packet = new DatagramPacket(raw, raw.length, this.ip, UDP_PORT);
		try (DatagramSocket datagrammSocket = new DatagramSocket()) {
			datagrammSocket.send(packet);
			return true;
		} catch (SocketException e) {
			this.logError(this.log, "Unable to open UDP socket for sending [" + s + "] to [" + this.ip.getHostAddress()
					+ "]: " + e.getMessage());
		} catch (IOException e) {
			this.logError(this.log,
					"Unable to send [" + s + "] UDP message to [" + this.ip.getHostAddress() + "]: " + e.getMessage());
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
		var b = new StringBuilder() //
				.append("L:").append(this.getActivePower().asString());
		if (!this.config.readOnly()) {
			b //
					.append("|SetCurrent:") //
					.append(this.channel(EvseChargePointKeba.ChannelId.DEBUG_SET_CHARGING_CURRENT).value().asString()) //
					.append("|SetEnable:") //
					.append(this.channel(EvseChargePointKeba.ChannelId.DEBUG_SET_ENABLE).value().asString());
		}
		return b.toString();
	}

	@Override
	public void apply(ChargePointActions actions) {
		this.utils.handleApplyCharge(actions);
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}

	protected void logDebug(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
