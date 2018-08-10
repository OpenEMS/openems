package market.agent.consumption.historybased;

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
import io.openems.edge.meter.api.SymmetricMeter;
import market.agent.api.MarketAgent;
import market.diagram.load.LoadDiagram;
import market.diagram.load.ValueDecimal;
import market.square.api.MarketSquare;

/**
 * A MarketAgent, which sets demand-bids based on the underlying meter's
 * history, thus it doesn't react to the markets current state. That is, because
 * the underlying meter can't be controlled. This agent's only purpose is to
 * inform other agents about the demand measured by the underlying meter. It
 * also generates a prediction by copying the last day's recording to the next
 * day.
 * 
 * @author FENECON GmbH
 *
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Market.Agent.Consumption.HistoryBased", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
public class HistoryBasedAgent extends AbstractOpenemsComponent implements MarketAgent, OpenemsComponent, EventHandler {

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected SymmetricMeter meter;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private MarketSquare market;

	@Reference
	protected ConfigurationAdmin cm;

	private Long id;

	private LoadDiagram consumption;

	private double speedFactor = 1;

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

	public HistoryBasedAgent() {
		this.consumption = new LoadDiagram();
		// init channels
		Stream.of( //
				Arrays.stream(HistoryBasedAgent.ChannelId.values()).map(channelId -> {
					return new IntegerReadChannel(this, channelId);
				})
		//
		).flatMap(channel -> channel).forEach(channel -> this.addChannel(channel));
	}

	@Activate
	void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.service_pid(), config.id(), config.enabled());

		// update filter for 'meter'
		if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "meter", config.meter_id())) {
			return;
		}

		this.speedFactor = config.speedFactor();

		id = market.registerAgent();

		market.addDiagram(id, consumption);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();

		try {
			market.unregisterAgent(id);
		} catch (NullPointerException e) {

		}
	}

	int powerSum = 0;
	int secCount = 0;
	long last = 0;

	private void calculateConsumption() {
		long now = System.currentTimeMillis();
		secCount++;
		powerSum += meter.getActivePower().value().get();

		if (secCount >= (int) (60 * speedFactor)) {
			secCount = 0;
			// record real time data
			consumption.setValue(new Date(last + 1), new Date(now),
					new ValueDecimal((double) powerSum / (60.0 * speedFactor)));
			powerSum = 0;
			// copy last minute to next day
			consumption.setValue(new Date(now + (long) (86400000L /* one day */ * speedFactor)),
					(long) (60000 /* one minute */ * speedFactor),
					consumption.getAvg(new Date(now - (long) (60000 /* one minute */ * speedFactor)),
							(long) (60000 /* one minute */ * speedFactor)));

			// erase everything older than two days
			consumption.erasePeriod(new Date(0), new Date(now - (long) (2 * 86400000L * speedFactor)));
		}

		this.channel(ChannelId.CURRENT_POWER).setNextValue(consumption.getValue(now).getDecimalDouble());
		this.channel(ChannelId.NEXT_POWER).setNextValue(consumption
				.getValue(1533891300000L + (long) (86400000L /* one day */ * speedFactor)).getDecimalDouble());
		last = now;
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
