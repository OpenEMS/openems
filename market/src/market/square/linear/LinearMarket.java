package market.square.linear;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
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
import market.diagram.api.Diagram;
import market.diagram.load.LoadDiagram;
import market.diagram.load.ValueDecimal;
import market.diagram.price.PriceDiagram;
import market.diagram.price.ValuePrice;
import market.square.api.MarketSquare;

/**
 * This MarketSquare implementation updates its market-state every during every
 * cycle. It processes the input in 15 minute blocks 24h ahead of real-time.
 * 
 * @author FENECON GmbH
 *
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Market.Square.Linear", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS)
public class LinearMarket extends AbstractOpenemsComponent implements MarketSquare, OpenemsComponent, EventHandler {

	@SuppressWarnings("rawtypes")
	private Map<Long, Map<Long, Diagram>> input;

	private PriceDiagram output;

	private PriceDiagram history;

	private double speedFactor;

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		LATEST_MARKET_REACTIVITY(new Doc().unit(Unit.NONE)), //
		LATEST_SOLD_POWER(new Doc().unit(Unit.WATT)), //
		LATEST_PRICE(new Doc().unit(Unit.NONE)); // TODO: create [1€/kWh]
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@SuppressWarnings("rawtypes")
	public LinearMarket() {
		input = new HashMap<Long, Map<Long, Diagram>>();
		output = new PriceDiagram();
		history = new PriceDiagram();
		speedFactor = 1;

		// init channels
		Stream.of( //
				Arrays.stream(LinearMarket.ChannelId.values()).map(channelId -> {
					return new IntegerReadChannel(this, channelId);
				})
		//
		).flatMap(channel -> channel).forEach(channel -> this.addChannel(channel));
	}

	@Activate
	void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.service_pid(), config.id(), config.enabled());

		this.speedFactor = config.speedFactor();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public PriceDiagram getMarketState() {
		return output;
	}

	public synchronized PriceDiagram getConsistentMarketState() {
		return output.getCopy();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public synchronized Long registerAgent() {
		Long agentID;
		do {
			agentID = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
		} while (input.containsKey(agentID));
		input.put(agentID, new HashMap<Long, Diagram>());
		return agentID;
	}

	@Override
	public synchronized void unregisterAgent(Long agentID) {
		input.remove(agentID);

	}

	@SuppressWarnings("rawtypes")
	@Override
	public synchronized Long addDiagram(Long agentID, Diagram d) {
		try {
			Long diagramID;
			do {
				diagramID = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
			} while (input.get(agentID).containsKey(diagramID));
			input.get(agentID).put(diagramID, d);
			return diagramID;
		} catch (NullPointerException e) {
			return -1L;
		}
	}

	@Override
	public synchronized void removeDiagram(Long agentID, Long diagramID) {
		try {
			input.get(agentID).remove(diagramID);
		} catch (NullPointerException e) {
		}
	}

	@Override
	public synchronized double getMarketReactivity(Date from, Date to) {
		// TODO: estimated value...calculate real average or find better
		// solution...e.g. currentPrice.getPowerDouble() /
		// currentPrice.getPriceDouble()
		return 1000;
	}

	@SuppressWarnings("rawtypes")
	private synchronized void calculateMarketState() {
		long now = System.currentTimeMillis();
		// delete outdated data

		input.forEach((agentID, diagramMap) -> {
			diagramMap.forEach((diagramID, d) -> {
				d.erasePeriod(new Date(0L), new Date(now - (long) (2 * 86400000L * speedFactor)));
			});
		});
		output.erasePeriod(new Date(0L), new Date(now - (long) (2 * 86400000L * speedFactor)));

		// store history for calculation of market-reactivity
		history = output.getCopy();

		// copy input to ensure consistency

		Map<Long, Map<Long, Diagram>> inputCopy = bytesToDiagrams(diagramsToBytes(input));

		// add up demand

		LoadDiagram demand = new LoadDiagram();
		// initialize values in current period so that checking for null becomes
		// irrelevant
		demand.setValue(now, (long) (86400000 * speedFactor), new ValueDecimal(0));

		// sum up demand (usually negative) and convert to positive value

		inputCopy.forEach((agentID, diagramMap) -> {
			diagramMap.forEach((diagramID, d) -> {
				if (d.getClass().equals(LoadDiagram.class)) {
					LoadDiagram ld = (LoadDiagram) d;
					// from now for 24h in 15min steps
					for (long l = now; l < now + (long) (86400000 * speedFactor); l += (long) (900000 * speedFactor)) {
						demand.setValue(l, (long) (900000 * speedFactor),
								demand.getAvg(l, (long) (900000 * speedFactor))
										.subtract(ld.getAvg(l, (long) (900000 * speedFactor))));
					}
				} else if (!d.getClass().equals(PriceDiagram.class)) {
					// illegal input
					diagramMap.remove(diagramID);
				}
			});
		});

		// from now for 24h in 15min steps
		for (long l = now; l < now + (long) (86400000 * speedFactor); l += (long) (900000 * speedFactor)) {
			final long counter = l; // copy l to final variable because ,err, java !

			// sum up supply
			List<ValuePrice> supplyList = new ArrayList<ValuePrice>();
			inputCopy.forEach((agentID, diagramMap) -> {
				diagramMap.forEach((diagramID, d) -> {
					if (d.getClass().equals(PriceDiagram.class)) {
						PriceDiagram pd = (PriceDiagram) d;
						supplyList.add(pd.getAvg(counter, (long) (900000 * speedFactor)));
					}
				});
			});
			// in case mains connection is too small we add a very expensive, but infinite
			// power-source, so that the market-system never fails
			supplyList.add(new ValuePrice(Integer.MAX_VALUE, Integer.MAX_VALUE));

			// sort supply sources by price
			Collections.sort(supplyList, (a, b) -> a.getPriceDouble() < b.getPriceDouble() ? -1
					: a.getPriceDouble() == b.getPriceDouble() ? 0 : 1);

			// calculate new market state
			ValueDecimal totalDemand = demand.getAvg(counter, (long) (900000 * speedFactor));
			for (ValuePrice s : supplyList) {
				if (l < 1534143100000L + (long) (86400000 * speedFactor)
						&& (l + (long) (900000 * speedFactor)) > 1534143100000L + (long) (86400000 * speedFactor)) {
				}
				demand.setValue(counter, (long) (900000 * speedFactor), demand
						.getAvg(counter, (long) (900000 * speedFactor)).subtract(new ValueDecimal(s.getPowerDouble())));
				// must be true at some point (at least with the virtual supply selling for
				// Double.Max_Value as price)
				if (demand.getAvg(counter, (long) (900000 * speedFactor)).getDecimalDouble() <= 0) {
					output.setValue(counter, (long) (900000 * speedFactor),
							new ValuePrice(s.getPriceDouble(), totalDemand.getDecimalDouble()));
					break;
				}
			}
		}
		this.channel(ChannelId.LATEST_SOLD_POWER)
				.setNextValue(output.getValue(1534143100000L + (long) (86400000 * speedFactor)).getPowerDouble());
		// TODO: find origin of those points you get in the LATEST_SOLD_POWER diagram
		// when watching a constant time at a small speedFactor. The diagram has regular
		// peaks like a saw,
		// although supply and demand are consistent. (Maybe this could be caused by
		// inconsistent writes to the diagrams, so that the getAvg() takes more and more
		// zero-value in account, but this should lead to more fluid waves.) However,
		// the most likely cause is that all the getAvg calls inside of this method's
		// for-next-day-in-15min-blocks-loop match two different write-blocks to a
		// changing degree, depending on the position in time, relative to the 15minute
		// (or with speedFactor 0.01666667 15second) block. Therefore this method should
		// only be executed at the beginning of each 15min period and/or properly
		// synchronized with the agent's write-blocks.
		this.channel(ChannelId.LATEST_PRICE)
				.setNextValue(output.getValue(1534143100000L + (long) (86400000 * speedFactor)).getPriceDouble() * 100);
		System.out.println(getMarketReactivity(new Date(1534143100000L + (long) (86400000 * speedFactor)),
				new Date(1534143100000L + (long) (86400000 * speedFactor) + (long) (900000 * speedFactor))));
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {

		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			calculateMarketState();
			break;
		}
	}

	@SuppressWarnings("rawtypes")
	private synchronized byte[] diagramsToBytes(Map<Long, Map<Long, Diagram>> input) {

		byte[] stream = null;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(baos);) {
			oos.writeObject(input);
			stream = baos.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return stream;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private synchronized Map<Long, Map<Long, Diagram>> bytesToDiagrams(byte[] bytes) {
		Map<Long, Map<Long, Diagram>> diagrams = null;

		try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
				ObjectInputStream ois = new ObjectInputStream(bais);) {
			diagrams = (Map<Long, Map<Long, Diagram>>) ois.readObject();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return diagrams;
	}
}
