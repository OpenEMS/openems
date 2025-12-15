package io.openems.edge.controller.evse.cluster;

import static io.openems.edge.controller.evse.cluster.EnergyScheduler.buildEnergyScheduleHandler;
import static io.openems.edge.controller.evse.cluster.RunUtils.calculate;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.List;
import java.util.Optional;
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

import com.google.common.collect.ImmutableMap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.ClusterScheduleContext;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.OptimizationContext;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.SingleModes;
import io.openems.edge.controller.evse.cluster.jsonrpc.GetSchedule;
import io.openems.edge.controller.evse.single.ControllerEvseSingle;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.handler.DifferentModes.Period;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.Controller.Cluster", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEvseClusterImpl extends AbstractOpenemsComponent
		implements OpenemsComponent, ControllerEvseCluster, Controller, ComponentJsonApi, EnergySchedulable {

	private final Logger log = LoggerFactory.getLogger(ControllerEvseClusterImpl.class);

	@Reference
	private ComponentManager componentManager;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private Sum sum;

	// TODO sort by configuration
	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = MULTIPLE)
	private volatile List<ControllerEvseSingle> ctrls = new CopyOnWriteArrayList<ControllerEvseSingle>();

	private Config config;
	private EshWithDifferentModes<SingleModes, OptimizationContext, ClusterScheduleContext> energyScheduleHandler;

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

		this.energyScheduleHandler = buildEnergyScheduleHandler(this, //
				() -> new EnergyScheduler.ClusterEshConfig(//
						this.config.distributionStrategy(), //
						this.config.enabled() //
								? this.ctrls.stream() //
										.collect(ImmutableMap.toImmutableMap(//
												OpenemsComponent::id, //
												ControllerEvseSingle::getParams))
								: ImmutableMap.of()));
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() {
		final var eshPeriod = this.energyScheduleHandler.getCurrentPeriod();
		final var eshMode = Optional.ofNullable(eshPeriod) //
				.map(Period::mode) //
				.orElse(null);

		calculate(this.componentManager.getClock(), this.config.distributionStrategy(), this.sum, this.ctrls, eshMode, //
				this.config.logVerbosity(), message -> this.logInfo(this.log, message)) //
				.streamEntries() //
				.filter(e -> e.params.combinedAbilities().chargePointAbilities() != null) //
				.forEach(e -> {
					// Apply actions
					e.ctrl.apply(e.actualMode, e.actions.build());
				});
	}

	@Override
	public EnergyScheduleHandler getEnergyScheduleHandler() {
		return this.energyScheduleHandler;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(new GetSchedule(), call -> {
			return GetSchedule.Response.create(call.getRequest(), this.energyScheduleHandler);
		});
	}
}
