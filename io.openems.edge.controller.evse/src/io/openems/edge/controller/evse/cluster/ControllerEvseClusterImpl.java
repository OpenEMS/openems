package io.openems.edge.controller.evse.cluster;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.edge.common.type.TypeUtils.fitWithin;
import static io.openems.edge.evse.api.EvseConstants.MIN_CURRENT;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

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

import com.google.common.collect.ImmutableList;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.evse.single.ControllerEvseSingle;
import io.openems.edge.controller.evse.single.Params;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint.ApplyCharge;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.chargepoint.Profile;
import io.openems.edge.evse.api.chargepoint.Profile.Command;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.Controller.Cluster", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEvseClusterImpl extends AbstractOpenemsComponent
		implements OpenemsComponent, ControllerEvseCluster, Controller {

	private final Logger log = LoggerFactory.getLogger(ControllerEvseClusterImpl.class);

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private Sum sum;

	// TODO sort by configuration
	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = MULTIPLE)
	private volatile List<ControllerEvseSingle> ctrls = new CopyOnWriteArrayList<ControllerEvseSingle>();

	private Config config;

	public ControllerEvseClusterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ControllerEvseCluster.ChannelId.values(), //
				Controller.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() {
		calculate(this.sum, this.ctrls, this::logDebug) //
				.forEach(o -> o.ctrl.apply(o.ac, o.commands));
	}

	private static record Input(ControllerEvseSingle ctrl, Params params) {
	}

	protected static record Output(ControllerEvseSingle ctrl, ApplyCharge ac, ImmutableList<Command> commands) {
	}

	protected static ImmutableList<Output> calculate(Sum sum, List<ControllerEvseSingle> ctrls,
			Consumer<String> logDebug) {
		var inputs = ctrls.stream() //
				.map(ctrl -> {
					var params = ctrl.getParams();
					if (params == null) {
						return null;
					}
					return new Input(ctrl, params);
				}) //
				.filter(Objects::nonNull) //
				.toList();

		final var outputs = ImmutableList.<Output>builder();
		var allParams = inputs.stream().map(i -> i.params()) //
				.collect(toImmutableList());
		var totalExcessPower = calculateTotalExcessPower(sum, allParams);
		var overallFixedPower = calculateOverallFixedPower(allParams);
		var remainingExcessPower = totalExcessPower - overallFixedPower;
		logDebug.accept("totalExcessPower=" + totalExcessPower + "; overallFixedPower=" + overallFixedPower
				+ "; remainingExcessPower=" + remainingExcessPower);

		for (var input : inputs) {
			final var ctrl = input.ctrl;
			final var params = input.params;

			// Handle Profile Commands
			final var commands = ImmutableList.<Profile.Command>builder();
			if (params.actualMode() == Mode.Actual.MINIMUM) {
				params.profiles().stream() //
						.filter(Profile.PhaseSwitchToSinglePhase.class::isInstance) //
						.map(Profile.PhaseSwitchToSinglePhase.class::cast) //
						.findFirst().ifPresent(phaseSwitch -> {
							// Switch from THREE to SINGLE phase in MINIMUM mode
							logDebug.accept(ctrl.id() + ": Switch from THREE to SINGLE phase in MINIMUM mode");
							commands.add(phaseSwitch.command());
						});

			} else if (params.actualMode() == Mode.Actual.FORCE) {
				params.profiles().stream() //
						.filter(Profile.PhaseSwitchToThreePhase.class::isInstance) //
						.map(Profile.PhaseSwitchToThreePhase.class::cast) //
						.findFirst().ifPresent(phaseSwitch -> {
							// Switch from SINGLE to THREE phase in FORCE mode
							logDebug.accept(ctrl.id() + ": Switch from SINGLE to THREE phase in FORCE mode");
							commands.add(phaseSwitch.command());
						});
			}

			// Evaluate Charge Current
			var ac = switch (params.actualMode()) {
			case ZERO -> ApplyCharge.ZERO;
			case MINIMUM -> new ApplyCharge.SetCurrent(MIN_CURRENT);
			case SURPLUS -> {
				var limit = params.limit();
				var maxPower = limit.getMaxPower();
				var minPower = limit.getMinPower();
				var power = params.isReadyForCharging() //
						? fitWithin(0, maxPower, remainingExcessPower) //
						: 0;
				remainingExcessPower -= power;
				var rawCurrent = limit.calculateCurrent(power);
				final int current;
				// TODO consider Non-Interruptable SURPLUS
				if (rawCurrent < limit.minCurrent()) {
					current = 0; // Not sufficient
				} else if (rawCurrent > limit.maxCurrent()) {
					current = limit.maxCurrent();
				} else {
					current = rawCurrent;
				}
				logDebug.accept(input.ctrl.id() + ": "//
						+ "isReadyForCharging=" + params.isReadyForCharging() + "; " //
						+ "limit=" + limit + "; " //
						+ "maxPower=" + maxPower + "; " //
						+ "minPower=" + minPower + "; " //
						+ "power=" + power + "; " //
						+ "remainingExcessPower=" + remainingExcessPower + "; " //
						+ "rawCurrent=" + rawCurrent + "; " //
						+ "current=" + current);
				if (current == 0) {
					yield ApplyCharge.ZERO;
				} else {
					yield new ApplyCharge.SetCurrent(current);
				}
			}
			case FORCE -> new ApplyCharge.SetCurrent(params.limit().maxCurrent());
			};

			outputs.add(new Output(ctrl, ac, commands.build()));
		}

		return outputs.build();
	}

	protected void logDebug(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	/**
	 * Calculates the total excess power, depending on the current PV production and
	 * house consumption.
	 * 
	 * @param sum    the {@link Sum} component
	 * @param params the {@link Params} of the {@link ControllerEvseSingle}s
	 * @return the available additional excess power for charging
	 */
	protected static int calculateTotalExcessPower(Sum sum, ImmutableList<Params> params) {
		var buyFromGrid = sum.getGridActivePower().orElse(0);
		var essDischarge = sum.getEssDischargePower().orElse(0);
		var evseCharge = params.stream() //
				.map(p -> p.activePower()) //
				.filter(Objects::nonNull) //
				.mapToInt(Integer::intValue) //
				.sum();

		return Math.max(0, evseCharge - buyFromGrid - essDischarge);
	}

	/**
	 * Calculates the overall fixed power.
	 * 
	 * @param params the {@link Params} of the {@link ControllerEvseSingle}s
	 * @return the fixed required power for MINIMUM and FORCE mode
	 */
	protected static int calculateOverallFixedPower(ImmutableList<Params> params) {
		return params.stream() //
				.filter(p -> p.isReadyForCharging()) //
				.map(p -> switch (p.actualMode()) {
				case FORCE -> p.limit().getMaxPower();
				case MINIMUM -> p.limit().getMinPower();
				case SURPLUS, ZERO -> null;
				}) //
				.filter(Objects::nonNull) //
				.mapToInt(Integer::intValue) //
				.sum();
	}
}
