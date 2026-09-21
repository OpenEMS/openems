package io.openems.edge.goodwe.battery.cluster;

import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.List;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.fenecon.home.BatteryFeneconHome;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.startstop.StartStoppable;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "GoodWe.BatteryCluster.FeneconHome", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE//
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
@GenerateTargetsFromReferences("Battery")
public class GoodWeBatteryClusterFeneconHomeImpl extends AbstractGoodWeBatteryCluster
		implements GoodWeBatteryCluster, OpenemsComponent, EventHandler, Battery, StartStoppable {

	private final ChannelManager channelManager = new ChannelManager(this);

	@Reference
	private ComponentManager componentManager;

	// field is not dynamic to reactivate whole component and also the ess so that
	// the ess is not going into stopped state
	@Reference(//
			policy = STATIC, policyOption = GREEDY, cardinality = MULTIPLE, //
			target = "(&(id=${config.battery_ids})(enabled=true))" //
	)
	protected synchronized void addBattery(BatteryFeneconHome battery) {
		super.addBatteries(List.of(battery));
	}

	protected synchronized void removeBattery(BatteryFeneconHome battery) {
		super.removeBatteries(List.of(battery));
	}

	public GoodWeBatteryClusterFeneconHomeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				GoodWeBatteryCluster.ChannelId.values() //
		);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.startStop());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.channelManager.deactivate();
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
	}

	@Override
	public String debugLog() {
		return new StringBuilder() //
				.append("Target: " + this.getStartStopTarget()) //
				.append("|StartStop:").append(this.getStartStop()) //
				.append("|SoC:").append(this.getSoc()) //
				.append("|Actual:").append(this.getVoltage()) //
				.append(";").append(this.getCurrent()) //
				.append("|Charge:").append(this.getChargeMaxVoltage()) //
				.append(";").append(this.getChargeMaxCurrent()) //
				.append("|Discharge:").append(this.getDischargeMinVoltage()) //
				.append(";").append(this.getDischargeMaxCurrent()) //
				.toString();
	}

	@Override
	protected ChannelManager getChannelManager() {
		return this.channelManager;
	}

}
