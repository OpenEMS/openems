package market.agent.consumption.requirementbased;

import java.io.IOException;
import java.util.Date;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.SymmetricMeter;
import market.agent.api.MarketAgent;
import market.diagram.api.Period;
import market.diagram.load.LoadDiagram;
import market.diagram.load.ValueDecimal;
import market.diagram.price.PriceDiagram;
import market.diagram.price.ValuePrice;
import market.square.api.MarketSquare;

/**
 * A MarketAgent, which sets demand-bids based on a given amount of energy, that
 * has to be available to the represented consumer until a given deadline has
 * passed. The agent tries to spread the given amount of energy in a way, that
 * the market's energy-price is equalized over time.
 * 
 * @author FENECON GmbH
 *
 */

// TODO: implement energyAim, deadline as channels
// TODO: implement max and minPower restrictions

@Designate(ocd = Config.class, factory = true)
@Component(name = "Market.Agent.Consumption.RequirementBased", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
public class RequirementBasedAgent extends AbstractOpenemsComponent
		implements MarketAgent, OpenemsComponent, EventHandler {

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected SymmetricMeter meter;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private MarketSquare market;

	private Long id;

	private LoadDiagram consumption;

	private LoadDiagram history;

	private PriceDiagram marketState;

	private double speedFactor = 1;

	double energyAim = 9; // 9 kWh

	Date deadline = new Date(System.currentTimeMillis() + (long) (21600000L * speedFactor)); // in 6 hours

	@Activate
	void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.service_pid(), config.id(), config.enabled());

		this.speedFactor = config.speedFactor();

		marketState = market.getConsistentMarketState();

		id = market.registerAgent();

		this.consumption = new LoadDiagram();
		history = consumption.getCopy();

		market.addDiagram(id, consumption);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();

		market.unregisterAgent(id);
	}

	int secCount = 0;
	int minCount = 0;

	private void calculateConsumption() {
		long now = System.currentTimeMillis();
		secCount++;

		if (secCount >= 60 * speedFactor) {
			secCount = 0;
			minCount++;
		}

		if (minCount >= 15) {
			minCount = 0;
			marketState = market.getConsistentMarketState();

			// subtract this agent's power from marketState
			marketState.setIterator(new Date(now));
			while (true) {
				Period<ValuePrice> next = marketState.getNext();
				if (next == null) {
					break;
				}
				ValueDecimal power = history.getAvg(next.getStart(), next.getEnd());
				double powerDouble;
				if (power == null) {
					powerDouble = 0.0;
				} else {
					powerDouble = power.getDecimalDouble();
				}
				marketState.setValue(next.getStart(), next.getEnd(),
						next.getValue().subtract(new ValuePrice(0.0, powerDouble)));
			}

			// calculate the average power registered at market, when this agent's demand
			// has been added
			double avgPowerAim = marketState.getAvg(new Date(now), deadline).getPowerDouble()
					+ energyAim / ((deadline.getTime() - now) / /* duration in hours */ 3600000);

			// calculate average price based on the average power and the market's
			// reactivity
			double avgPriceAim = avgPowerAim / Math.max(0.1, market.getMarketReactivity(new Date(now), deadline)
					- marketState.getAvg(new Date(now), deadline).getPriceDouble());

			// save consumption history
			history = consumption.getCopy();

			// for each 15 min block calculate the estimated power, that is needed to
			// increase/reduce to price to the calculated average
			for (long l = now; l < now
					+ (long) (86400000L * speedFactor) /* one day */; l += (long) (60000 * speedFactor)/* 1 min */) {
				ValuePrice currentPrice = marketState.getAvg(l, (long) (60000 * speedFactor));
				if (currentPrice == null) {
					currentPrice = new ValuePrice(0, 0);
				}

				ValueDecimal maxNewPower = new ValueDecimal(
						market.getMarketReactivity(new Date(l), new Date(now + (long) (60000 * speedFactor)))
								* (avgPriceAim - currentPrice.getPriceDouble()));

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
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {

		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			calculateConsumption();
			break;
		}
	}
}