package io.openems.edge.evse.chargepoint.smartfox;

import static io.openems.common.bridge.http.api.HttpMethod.GET;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static java.lang.Math.round;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.IntUtils;
import io.openems.common.utils.LatestWinsFutureExecutor;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ChargePoint.Smartfox", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class EvseChargePointSmartfoxImpl extends AbstractOpenemsModbusComponent
		implements EvseChargePointSmartfox, EvseChargePoint, ElectricityMeter, ModbusComponent, OpenemsComponent,
		TimedataProvider, EventHandler {

	/*
	 * SMARTFOX Energy Manager Modbus register map ("Modbus Register SMARTFOX Pro &
	 * SMARTFOX Pro 2"). Documented addresses are 1-based, i.e. register 41600 is
	 * element 41599. Each Car Charger occupies a block of 10 registers:
	 * - +0..3: energy total [Wh] (uint64)
	 * - +4..5: energy of current charge session [Wh] (uint32)
	 * - +6..7: charge power [W] (uint32)
	 * - +8: charge mode (0 = surplus, 1 = manual)
	 * - +9: manual charging value [%]
	 *
	 * Writes to the charge-mode registers are not reliably accepted by all
	 * firmwares (the Energy Manager overwrites them for chargers attached via
	 * RS485). Control therefore uses the same local HTTP API as the SMARTFOX web
	 * UI: GET /setswcc.cgi?num={index}&mode={mode}&value={percent}.
	 */
	private static final int REGISTERS_PER_CHARGE_POINT = 10;
	private static final int FIRST_CHARGE_POINT_ADDRESS = 41599;
	private static final int HTTP_MODE_MANUAL = 1;

	private final Logger log = LoggerFactory.getLogger(EvseChargePointSmartfoxImpl.class);
	private final LatestWinsFutureExecutor targetExecutor = new LatestWinsFutureExecutor();

	private Config config;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public EvseChargePointSmartfoxImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvseChargePoint.ChannelId.values(), //
				EvseChargePointSmartfox.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());
		this.applyConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.applyConfig(config);
	}

	private void applyConfig(Config config) {
		this.config = config;
		if (!config.readOnly() && this.httpBridge == null) {
			this.httpBridge = this.httpBridgeFactory.get();
		} else if (config.readOnly() && this.httpBridge != null) {
			this.httpBridgeFactory.unget(this.httpBridge);
			this.httpBridge = null;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.targetExecutor.cancel();
		if (this.httpBridge != null) {
			this.httpBridgeFactory.unget(this.httpBridge);
			this.httpBridge = null;
		}
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		final var base = FIRST_CHARGE_POINT_ADDRESS
				+ (this.config.chargePointNumber() - 1) * REGISTERS_PER_CHARGE_POINT;
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(base, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY,
								new UnsignedQuadruplewordElement(base)), //
						m(EvseChargePointSmartfox.ChannelId.ENERGY_SESSION,
								new UnsignedDoublewordElement(base + 4)), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedDoublewordElement(base + 6)), //
						m(EvseChargePointSmartfox.ChannelId.CHARGE_MODE, new UnsignedWordElement(base + 8)), //
						m(EvseChargePointSmartfox.ChannelId.MAN_CHARGING_VALUE, new UnsignedWordElement(base + 9))));
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE ->
			// SMARTFOX registers do not expose an EV plug/ready state; the Charge-Point
			// is assumed ready as long as communication with the Energy Manager works.
			setValue(this, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING,
					!this.getModbusCommunicationFailedChannel().getNextValue().orElse(true));
		}
	}

	@Override
	public ChargePointAbilities getChargePointAbilities() {
		var config = this.config;
		if (config == null || config.readOnly()) {
			return null;
		}
		return ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Watt(THREE_PHASE, 0, config.maxChargingPower(),
						Math.max(1, config.maxChargingPower() / 100))) //
				.build();
	}

	@Override
	public void apply(ChargePointActions actions) {
		if (this.httpBridge == null) {
			return;
		}
		final var percent = IntUtils.fitWithin(0, 100,
				round(actions.getApplySetPointInWatt().value() * 100f / this.config.maxChargingPower()));
		try {
			this.targetExecutor.execute(() -> this.dispatchSetPoint(percent), (response, error) -> {
				if (error != null) {
					this.log.warn("Failed to apply charge set-point: " + error.getMessage());
				}
			});
		} catch (RejectedExecutionException e) {
			this.log.warn("Cannot dispatch charge set-point: " + e.getMessage());
		}
	}

	private CompletableFuture<HttpResponse<String>> dispatchSetPoint(int percent) {
		return this.httpBridge.request(BridgeHttp.create("http://" + this.config.ip() + "/setswcc.cgi" //
				+ "?num=" + (this.config.chargePointNumber() - 1) //
				+ "&mode=" + HTTP_MODE_MANUAL //
				+ "&value=" + percent) //
				.setMethod(GET) //
				.build());
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
	public boolean isReadOnly() {
		return this.config.readOnly();
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}
}
