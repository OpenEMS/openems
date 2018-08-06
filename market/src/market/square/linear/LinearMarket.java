package market.square.linear;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import market.diagram.api.Diagram;
import market.diagram.load.LoadDiagram;
import market.diagram.load.ValueDecimal;
import market.diagram.price.PriceDiagram;
import market.diagram.price.ValuePrice;
import market.square.api.MarketSquare;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Market.Square.Linear", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class LinearMarket extends AbstractOpenemsComponent implements MarketSquare, OpenemsComponent {

	@SuppressWarnings("rawtypes")
	private Map<Long, Map<Long, Diagram>> input;

	private PriceDiagram output;

	private PriceDiagram history;

	private double speedFactor;

	@SuppressWarnings("rawtypes")
	public LinearMarket() {
		input = new HashMap<Long, Map<Long, Diagram>>();
		output = new PriceDiagram();
		speedFactor = 1;
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
	public synchronized double getMarketReactivity(Date at, Date to) {
		long now = System.currentTimeMillis();
		if (output.getAvg(now, 86400000L).getPriceDouble() - history.getAvg(now, 86400000L).getPriceDouble() != 0
				&& output.getAvg(now, 86400000L).getPowerDouble()
						- history.getAvg(now, 86400000L).getPowerDouble() != 0) {
			return Math.abs(
					(output.getAvg(now, 86400000L).getPowerDouble() - history.getAvg(now, 86400000L).getPowerDouble())
							/ (output.getAvg(now, 86400000L).getPriceDouble()
									- history.getAvg(now, 86400000L).getPriceDouble()));
		} else {
			return 1000; // TODO: estimated value...calculate real average or find better
							// solution...e.g. currentPrice.getPowerDouble() /
							// currentPrice.getPriceDouble()
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void run() {
		while (true) {
			long now = System.currentTimeMillis();
			// delete outdated data

			input.forEach((agentID, diagramMap) -> {
				diagramMap.forEach((diagramID, d) -> {
					d.erasePeriod(new Date(0L), new Date(now - (long) (60000 * speedFactor)));
				});
			});
			output.erasePeriod(new Date(0L), new Date(now - (long) (60000 * speedFactor)));

			// store history for calculation of market-reactivity
			history = output.getCopy();

			// copy input to ensure consistency

			byte[] inputBytes = diagramsToBytes(input);
			Map<Long, Map<Long, Diagram>> inputCopy = bytesToDiagrams(inputBytes);

			// add up demand

			LoadDiagram demand = new LoadDiagram();
			// initialize values in current period so that checking for null becomes
			// irrelevant
			demand.setValue(now, (long) (86400000 * speedFactor), new ValueDecimal(0));

			inputCopy.forEach((agentID, diagramMap) -> {
				diagramMap.forEach((diagramID, d) -> {
					if (d.getClass().equals(LoadDiagram.class)) {
						LoadDiagram ld = (LoadDiagram) d;
						// from now for 24h in 15min steps
						for (long l = now; l < now
								+ (long) (86400000 * speedFactor); l += (long) (900000 * speedFactor)) {
							ValueDecimal avg = demand.getAvg(l, (long) (900000 * speedFactor));
							if (avg != null) {
								demand.setValue(l, (long) (900000 * speedFactor),
										avg.add(ld.getAvg(l, (long) (900000 * speedFactor))));
							}
						}
					} else if (!d.getClass().equals(PriceDiagram.class)) {
						// illegal input
						diagramMap.remove(diagramID);
					}
				});
			});
			// calculate new market state

			// from now for 24h in 15min steps
			for (long l = now; l < now + (long) (86400000 * speedFactor); l += (long) (900000 * speedFactor)) {
				final long counter = l; // copy l to final variable because ,err, java !
				List<ValuePrice> supplyList = new ArrayList<ValuePrice>();
				inputCopy.forEach((agentID, diagramMap) -> {
					diagramMap.forEach((diagramID, d) -> {
						if (d.getClass().equals(PriceDiagram.class)) {
							PriceDiagram pd = (PriceDiagram) d;
							ValuePrice avg = pd.getAvg(counter, (long) (900000 * speedFactor));
							if (avg != null) {
								supplyList.add(avg);
							}
						}
					});
				});
				// in case mains connection is too small
				supplyList.add(new ValuePrice(Double.MAX_VALUE, Double.MAX_VALUE));

				// sort supply sources by price
				Collections.sort(supplyList, (a, b) -> a.getPriceDouble() < b.getPriceDouble() ? -1
						: a.getPriceDouble() == b.getPriceDouble() ? 0 : 1);

				ValueDecimal totalDemand = demand.getAvg(counter, (long) (900000 * speedFactor));
				double pricePowerSum = 0.0;
				for (ValuePrice s : supplyList) {
					demand.setValue(counter, (long) (900000 * speedFactor),
							demand.getAvg(counter, (long) (900000 * speedFactor))
									.subtract(new ValueDecimal(s.getPowerDouble())));
					// must be true (at least with the virtual supply selling for Double.Max_Value
					// as price)
					if (demand.getAvg(counter, (long) (900000 * speedFactor)).getDecimalDouble() <= 0) {
						pricePowerSum += s.getPriceDouble() * (s.getPowerDouble()
								- demand.getAvg(counter, (long) (900000 * speedFactor)).getDecimalDouble());
						// TODO: decide, whether to use final or average price
						output.setValue(counter, (long) (900000 * speedFactor), new ValuePrice(
								pricePowerSum / totalDemand.getDecimalDouble(), totalDemand.getDecimalDouble()));
						break;
					} else {
						pricePowerSum += s.getPriceDouble() * s.getPowerDouble();
					}
				}
			}
			System.out.println(output.getValue(new Date(System.currentTimeMillis())));

			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	// TODO: remove this note
	// https://howtodoinjava.com/core-java/serialization/how-to-do-deep-cloning-using-in-memory-serialization-in-java/

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
