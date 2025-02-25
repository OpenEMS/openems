package io.openems.edge.controller.evse.single;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.time.Clock;
import java.util.function.BiConsumer;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.evse.api.Limit;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.chargepoint.Status;
import io.openems.edge.evse.api.electricvehicle.EvseElectricVehicle;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.Controller.Single", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEvseSingleImpl extends AbstractOpenemsComponent
		implements Controller, ControllerEvseSingle, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ControllerEvseSingleImpl.class);

	private BiConsumer<Value<Status>, Value<Status>> onChargePointStatusChange;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private EvseChargePoint chargePoint;

	// TODO Optional Reference
	@Reference
	private EvseElectricVehicle electricVehicle;

	private Config config;

	public ControllerEvseSingleImpl() {
		this(Clock.systemDefaultZone());
	}

	protected ControllerEvseSingleImpl(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEvseSingle.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "chargePoint",
				config.chargePoint_id())) {
			return;
		}
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "electricVehicle",
				config.electricVehicle_id())) {
			return;
		}
		if (config.mode() == Mode.SMART) {
			this.chargePoint.getStatusChannel().onChange(this.onChargePointStatusChange);
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.chargePoint.getStatusChannel().removeOnChangeCallback(this.onChargePointStatusChange);
		super.deactivate();
	}

	@Override
	public Params getParams() {
		Mode.Actual actualMode = switch (this.config.mode()) {
		case FORCE -> Mode.Actual.FORCE;
		case ZERO -> Mode.Actual.ZERO;
		case MINIMUM -> Mode.Actual.MINIMUM;
		case SMART -> null; // TODO for Time-of-Use
		};

		// Is Ready for Charging?
		var readyForCharging = switch (this.chargePoint.getStatus()) {
		case UNDEFINED, CHARGING_REJECTED, ENERGY_LIMIT_REACHED, ERROR, NOT_READY_FOR_CHARGING //
			-> true;
		case READY_FOR_CHARGING, CHARGING, STARTING //
			-> false;
		};

		var chargePoint = this.chargePoint.getChargeParams();
		var electricVehicle = this.electricVehicle.getChargeParams();
		var limits = mergeLimits(chargePoint, electricVehicle);
		this.logDebug("ActualMode:" + actualMode + "|ChargeParams:" + limits);

		if (limits == null) {
			return null;
		}

		return new Params(readyForCharging, actualMode, limits, chargePoint.applyChargeCallback());
	}

	@Override
	public void run() throws OpenemsNamedException {
		// Actual logic is carried out in the EVSE Cluster
	}

	protected static final Limit mergeLimits(EvseChargePoint.ChargeParams chargePoint,
			EvseElectricVehicle.ChargeParams electricVehicle) {
		if (chargePoint == null || electricVehicle == null) {
			return null;
		}
		var cp = chargePoint.limit();
		return electricVehicle.limits().stream() //
				.filter(ev -> ev.phase() == cp.phase()) //
				.findFirst() //
				.map(ev -> new Limit(cp.phase(), //
						max(cp.minCurrent(), ev.minCurrent()), //
						min(cp.maxCurrent(), ev.maxCurrent()))) //
				.orElse(null);
	}

	protected void logDebug(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}
}
