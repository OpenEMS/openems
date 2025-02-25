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
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.evse.single.ControllerEvseSingle;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint.ApplyCharge;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.Controller.Cluster", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS, //
})
public class ControllerEvseClusterImpl extends AbstractOpenemsComponent
		implements OpenemsComponent, ControllerEvseCluster, Controller {

	@Reference
	private ConfigurationAdmin cm;

	// TODO sort by configuration
	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = MULTIPLE)
	private volatile List<ControllerEvseSingle> ctrls = new CopyOnWriteArrayList<ControllerEvseSingle>();

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
			var ac = switch (params.actualMode()) {
			case ZERO -> ApplyCharge.ZERO;
			case MINIMUM -> new ApplyCharge.SetCurrent(MIN_CURRENT);
			case FORCE -> new ApplyCharge.SetCurrent(params.limit().maxCurrent());
			};

			params.applyCharge(ac);
		}
	}
}
