package io.openems.edge.simulator.predictor;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.predictor.api.prediction.AbstractPredictor;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.prediction.Predictor;
import io.openems.edge.simulator.datasource.api.SimulatorDatasource;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.Predictor", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class SimulatorPredictorImpl extends AbstractPredictor
		implements SimulatorPredictor, Predictor, OpenemsComponent {

	@Reference
	private ComponentManager componentManager;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SimulatorDatasource datasource;

	public SimulatorPredictorImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SimulatorPredictor.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.channelAddresses(),
				config.logVerbosity());

		// update filter for 'datasource'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "datasource", config.datasource_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ClockProvider getClockProvider() {
		return this.componentManager;
	}

	@Override
	protected Prediction createNewPrediction(ChannelAddress channelAddress) {
		var source = this.datasource.<Integer>getValues(OpenemsType.INTEGER, channelAddress);
		if (source.isEmpty()) {
			return Prediction.EMPTY_PREDICTION;
		}
		// Fill 48 hours starting from midnight; assume Datasource provides one vale per
		// 5 minutes
		var values = new Integer[48 /* hours */ * 4 /* quarters per hour */];
		var cache = new LinkedList<Integer>();
		for (var i = 0; i < values.length; i++) {
			while (cache.size() < 3) {
				cache.addAll(source);
			}
			values[i] = TypeUtils.averageInt(cache.poll(), cache.poll(), cache.poll());
		}
		var today = ZonedDateTime.now(this.componentManager.getClock()).truncatedTo(ChronoUnit.DAYS);
		return Prediction.from(today, values);
	}
}
