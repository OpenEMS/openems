package market.agent.production.historybased;

import java.io.IOException;
import java.util.Date;

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
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.SymmetricMeter;
import market.agent.api.MarketAgent;
import market.diagram.price.PriceDiagram;
import market.diagram.price.ValuePrice;
import market.square.api.MarketSquare;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Market.Agent.Production.HistoryBased", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class HistoryBasedAgent extends AbstractOpenemsComponent implements MarketAgent, OpenemsComponent {

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected SymmetricMeter meter;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private MarketSquare market;

	@Reference
	protected ConfigurationAdmin cm;

	private Long id;

	private PriceDiagram production;

	private double speedFactor = 1;

	private double price;

	@Activate
	void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		System.out.println("activating HistoryBasedAgent");

		// update filter for 'meter'
		if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "Meter", config.meter_id())) {
			return;
		}

		this.speedFactor = config.speedFactor();
		this.price = config.price();

		id = market.registerAgent();

		this.production = new PriceDiagram();

		market.addDiagram(id, production);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();

		market.unregisterAgent(id);
	}

	@Override
	public void run() {
		int powerSum = 0;
		int secCount = 0;
		int minCount = 0;
		while (true) {
			long now = System.currentTimeMillis();
			secCount++;
			powerSum += meter.getActivePower().value().get();

			// record real time data
			if (secCount >= 60 * speedFactor) {
				secCount = 0;
				minCount++;
				production.setValue(new Date(now - (long) (60000 * speedFactor)), new Date(now),
						new ValuePrice(price, (double) powerSum / (60.0 * speedFactor)));
			}

			// copy history to next day
			if (minCount >= 15) {
				minCount = 0;
				for (long l = now; l < now + (long) (86400000L * speedFactor) /* one day */; l += (long) (60000
						* speedFactor)/* 1 min */) {
					ValuePrice record = production.getAvg(l - (long) (86400000L * speedFactor) /* one day */,
							(long) (60000 * speedFactor));
					if (record != null) {
						production.setValue(l, (long) (60000 * speedFactor), record);
					}
				}
				// erase everything older than a day
				production.erasePeriod(new Date(0), new Date(now - (long) (86400000L * speedFactor)));
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
