package market.agent.grid;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Stream;

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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import market.agent.api.MarketAgent;
import market.diagram.load.LoadDiagram;
import market.diagram.load.ValueDecimal;
import market.diagram.price.PriceDiagram;
import market.diagram.price.ValuePrice;
import market.square.api.MarketSquare;

/**
 * A MarketAgent, which represents the mains connection at the internal
 * energy-market. It consumes energy, as long as the price is lower than the
 * Config.sellPrice() and produces energy, when the market-price is higher than
 * the Config.buyPrice().
 * 
 * @author FENECON GmbH
 *
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Market.Agent.Grid", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
public class GridAgent extends AbstractOpenemsComponent implements MarketAgent, OpenemsComponent, EventHandler {

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private MarketSquare market;

	@Reference
	protected ConfigurationAdmin cm;

	private Long id;

	private PriceDiagram production;

	private LoadDiagram consumption;

	private double speedFactor = 1;

	private double sellPrice;

	private double buyPrice;

	private int maxPower;

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		CURRENT_POWER(new Doc().unit(Unit.WATT)), //
		NEXT_POWER(new Doc().unit(Unit.WATT));
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	public GridAgent() {
		this.production = new PriceDiagram();
		this.consumption = new LoadDiagram();
		// init channels
		Stream.of( //
				Arrays.stream(GridAgent.ChannelId.values()).map(channelId -> {
					return new IntegerReadChannel(this, channelId);
				})
		//
		).flatMap(channel -> channel).forEach(channel -> this.addChannel(channel));
	}

	@Activate
	void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.service_pid(), config.id(), config.enabled());

		this.speedFactor = config.speedFactor();
		this.sellPrice = config.sellPrice();
		this.buyPrice = config.buyPrice();
		this.maxPower = config.maxPower();

		id = market.registerAgent();

		market.addDiagram(id, production);
		market.addDiagram(id, consumption);

		// always offer full capacity for buyPrice
		production.setValue(new Date(0), new Date(Long.MAX_VALUE), new ValuePrice(buyPrice, maxPower));
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();

		market.unregisterAgent(id);
	}

	int secCount = 0;
	long last = 0;

	private void calculateConsumption() {
		long now = System.currentTimeMillis();
		secCount++;
		if (secCount >= (int) (900 * speedFactor)) {
			secCount = 0;
			// from now 24h in 15min steps
			for (long l = now; l < now + (long) (86400000 * speedFactor); l += (long) (900000 * speedFactor)) {
				// consumption is negative -> negative powerDelta value equals increase in
				// consumption and price
				double powerDelta = 0.0;
				powerDelta = 1000 /*
									 * market.getMarketReactivity(new Date(l), new Date(l + (long) (900000 *
									 * speedFactor)))
									 */
						* /* proportional to price-difference */ (market.getMarketState()
								.getAvg(new Date(l), (long) (900000 * speedFactor)).getPriceDouble() - sellPrice)
						* /* proportional to time being left */ ((l - now) / (86400000 * speedFactor));
				ValueDecimal newPower = consumption.getAvg(new Date(l), (long) (900000 * speedFactor))
						.add(new ValueDecimal(powerDelta));
				if (newPower.getDecimalDouble() > 0) {
					newPower = new ValueDecimal(0);
				}
				consumption.setValue(new Date(l), (long) (900000 * speedFactor), newPower);
			}

			// erase everything older than two days
			consumption.erasePeriod(new Date(0), new Date(now - (long) (2 * 86400000L * speedFactor)));
			production.erasePeriod(new Date(0), new Date(now - (long) (2 * 86400000L * speedFactor)));
		}
		this.channel(ChannelId.CURRENT_POWER).setNextValue(consumption.getValue(now).getDecimalDouble());
		this.channel(ChannelId.NEXT_POWER).setNextValue(
				consumption.getValue(1534144900000L + (long) (86400000L * speedFactor)).getDecimalDouble());
		last = now;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {

		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			// production is offered statically -> see activate()
			calculateConsumption();
			break;
		}
	}
}
