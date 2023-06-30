package io.openems.edge.evcs.core;

import java.util.Map;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.filter.DisabledRampFilter;
import io.openems.edge.common.filter.RampFilter;
import io.openems.edge.evcs.api.EvcsPower;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = EvcsPowerComponent.SINGLETON_SERVICE_PID, //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.OPTIONAL, //
		property = { //
				"enabled=true", //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE //
})
public class EvcsPowerComponent extends AbstractOpenemsComponent implements OpenemsComponent, EvcsPower {

	public static final String SINGLETON_SERVICE_PID = "Evcs.SlowPowerIncreaseFilter";
	public static final String SINGLETON_COMPONENT_ID = "_evcsSlowPowerIncreaseFilter";

	@Reference
	private ConfigurationAdmin cm;

	private RampFilter rampFilter;
	private float increaseRate;

	public EvcsPowerComponent() {
		super(//
				OpenemsComponent.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Map<String, Object> properties, Config config) {
		super.activate(context, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
		this.updateConfig(config);

		if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		super.modified(context, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
		this.updateConfig(config);

		if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void updateConfig(Config config) {
		this.increaseRate = config.increaseRate();
		if (config.enableSlowIncrease()) {
			this.rampFilter = new RampFilter();
		} else {
			this.rampFilter = new DisabledRampFilter();
		}
	}

	@Override
	public RampFilter getRampFilter() {
		return this.rampFilter;
	}

	@Override
	public float getIncreaseRate() {
		return this.increaseRate;
	}
}
