package io.openems.edge.controller.evse.cluster;

import static io.openems.edge.evse.api.EvseConstants.MIN_CURRENT;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.evse.single.ControllerEvseSingle;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint.ApplyCharge;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.chargepoint.Profile;

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
	public void run() throws OpenemsNamedException {
		for (var ctrl : this.ctrls) {
			var params = ctrl.getParams();
			if (params == null) {
				continue;
			}

			// Handle Profile Commands
			final var commands = ImmutableList.<Profile.Command>builder();
			if (params.actualMode() == Mode.Actual.MINIMUM) {
				params.profiles().stream() //
						.filter(Profile.PhaseSwitchToSinglePhase.class::isInstance) //
						.map(Profile.PhaseSwitchToSinglePhase.class::cast) //
						.findFirst().ifPresent(phaseSwitch -> {
							// Switch from THREE to SINGLE phase in MINIMUM mode
							this.logDebug(ctrl.id() + ": Switch from THREE to SINGLE phase in MINIMUM mode");
							commands.add(phaseSwitch.command());
						});

			} else if (params.actualMode() == Mode.Actual.FORCE) {
				params.profiles().stream() //
						.filter(Profile.PhaseSwitchToThreePhase.class::isInstance) //
						.map(Profile.PhaseSwitchToThreePhase.class::cast) //
						.findFirst().ifPresent(phaseSwitch -> {
							// Switch from SINGLE to THREE phase in FORCE mode
							this.logDebug(ctrl.id() + ": Switch from SINGLE to THREE phase in FORCE mode");
							commands.add(phaseSwitch.command());
						});
			}

			// Evaluate Charge Current
			var ac = switch (params.actualMode()) {
			case ZERO -> ApplyCharge.ZERO;
			case MINIMUM -> new ApplyCharge.SetCurrent(MIN_CURRENT);
			case FORCE -> new ApplyCharge.SetCurrent(params.limit().maxCurrent());
			};

			ctrl.apply(ac, commands.build());
		}
	}

	protected void logDebug(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}
}
