package io.openems.edge.ess.power;

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

import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.ess.symmetric.api.ManagedSymmetricEss;

// TODO: it appears that this component is started/executed twice for each Event
@Component( //
		immediate = true, //
		scope = ServiceScope.SINGLETON, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE //
		})
public class ManagedEssComponent implements EventHandler {

	// private final Logger log =
	// LoggerFactory.getLogger(ManagedEssComponent.class);

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	private volatile List<ManagedSymmetricEss> components = new CopyOnWriteArrayList<>();

	@Override
	public void handleEvent(Event event) {
		for (ManagedSymmetricEss component : this.components) {
			Power power = component.getPower();
			switch (event.getTopic()) {
			case EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE:
				power.applyPower();
				break;
			case EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE:
				power.clearCycleConstraints();
				break;
			}
		}
	}
}
