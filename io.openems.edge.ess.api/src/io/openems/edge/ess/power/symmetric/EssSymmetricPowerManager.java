package io.openems.edge.ess.power.symmetric;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import io.openems.edge.common.controllerexecutor.EdgeEventConstants;
import io.openems.edge.ess.symmetric.api.EssSymmetric;

/**
 * This helper component handles the ControllerExecuter Cycle for SymmetricPower
 * objects.
 * 
 * @author stefan.feilmeier
 */
@Component(scope = ServiceScope.SINGLETON, property = { //
		EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE,
		EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE })
public class EssSymmetricPowerManager implements EventHandler {

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	private volatile List<EssSymmetric> components = new CopyOnWriteArrayList<>();

	@Override
	public void handleEvent(Event event) {
		for (EssSymmetric component : this.components) {
			SymmetricPower power = component.getPower();
			switch (event.getTopic()) {
			case EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE:
				power.onTopicCycleBeforeWrite();
				break;
			case EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE:
				power.onTopicCycleAfterWrite();
				break;
			}
		}
	}
}
