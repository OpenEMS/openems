package market.agent.consumption.pricebased;

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
import market.diagram.load.LoadDiagram;
import market.diagram.load.ValueDecimal;
import market.diagram.price.PriceDiagram;
import market.diagram.price.ValuePrice;
import market.square.api.MarketSquare;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Market.Agent.Consumption.PriceBased", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class PriceBasedAgent extends AbstractOpenemsComponent implements MarketAgent, OpenemsComponent {

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected SymmetricMeter meter;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private MarketSquare market;

	@Reference
	protected ConfigurationAdmin cm;

	private Long id;

	private LoadDiagram consumption;

	private PriceDiagram marketState;

	private double speedFactor = 1;

	double price;

	@Activate
	void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.service_pid(), config.id(), config.enabled());

		// update filter for 'meter'
		if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "Meter", config.meter_id())) {
			return;
		}

		this.speedFactor = config.speedFactor();
		this.price = config.price();

		marketState = market.getConsistentMarketState();

		id = market.registerAgent();

		this.consumption = new LoadDiagram();

		market.addDiagram(id, consumption);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();

		market.unregisterAgent(id);
	}

	@Override
	public void run() {
		int secCount = 0;
		int minCount = 0;
		while (true) {
			long now = System.currentTimeMillis();
			secCount++;

			if (secCount >= 60 * speedFactor) {
				secCount = 0;
				minCount++;
			}

			if (minCount >= 15) {
				minCount = 0;
				marketState = market.getConsistentMarketState();
				for (long l = now; l < now + (long) (86400000L * speedFactor) /* one day */; l += (long) (60000
						* speedFactor)/* 1 min */) {
					ValuePrice currentPrice = marketState.getAvg(l, (long) (60000 * speedFactor));
					if (currentPrice == null) {
						currentPrice = new ValuePrice(0, 0);
					}

					ValueDecimal maxNewPower = new ValueDecimal(consumption.getAvg(l, (long) (60000 * speedFactor))
							.getDecimalDouble()
							+ market.getMarketReactivity(new Date(l), new Date(now + (long) (60000 * speedFactor)))
									* (price - currentPrice.getPriceDouble()));

					ValueDecimal newPower = maxNewPower;
					if (maxNewPower.getDecimalDouble() < 0) {
						newPower = new ValueDecimal(0);
					}
					// TODO: min and max power

					consumption.setValue(l, (long) (60000 * speedFactor), newPower);
				}
				// erase everything older than a day
				consumption.erasePeriod(new Date(0), new Date(now - (long) (86400000L * speedFactor)));
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
