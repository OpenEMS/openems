package io.openems.edge.predictor.persistencemodel.production;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;

import org.junit.Test;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;

import io.openems.common.OpenemsConstants;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.predictor.api.HourlyPrediction;

public class ProductionPredictorTest {

	@Test
	public void test() {
		TimeLeapClock clock = new TimeLeapClock();
		ProductionPredictor predictor = new ProductionPredictor(clock);
		DummyComponentManager componentManager = new DummyComponentManager();
		predictor.componentManager = componentManager;
		SimulatedSum sum = new SimulatedSum();
		componentManager.addComponent(sum);
		Event event = new Event(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, new HashMap<>());

		sum.productionActiveEnergy.setNextValue(1000);
		sum.productionActiveEnergy.nextProcessImage();
		predictor.handleEvent(event);

		clock.leap(1, ChronoUnit.MINUTES);
		sum.productionActiveEnergy.setNextValue(1100);
		sum.productionActiveEnergy.nextProcessImage();
		predictor.handleEvent(event);

		clock.leap(1, ChronoUnit.HOURS);
		sum.productionActiveEnergy.setNextValue(2000);
		sum.productionActiveEnergy.nextProcessImage();
		predictor.handleEvent(event);

		clock.leap(1, ChronoUnit.HOURS);
		sum.productionActiveEnergy.setNextValue(4000);
		sum.productionActiveEnergy.nextProcessImage();
		predictor.handleEvent(event);

		clock.leap(1, ChronoUnit.HOURS);
		sum.productionActiveEnergy.setNextValue(5500);
		sum.productionActiveEnergy.nextProcessImage();
		predictor.handleEvent(event);

		HourlyPrediction p = predictor.get24hPrediction();
		assertEquals(p.getStart(), LocalDateTime.now(clock).withNano(0).withMinute(0).withSecond(0));

		Integer[] v = p.getValues();
		assertEquals(v.length, 24);

		assertEquals(null, v[0]);
		assertEquals(null, v[1]);
		assertEquals(null, v[2]);
		assertEquals(null, v[3]);
		assertEquals(null, v[4]);
		assertEquals(null, v[5]);
		assertEquals(null, v[6]);
		assertEquals(null, v[7]);
		assertEquals(null, v[8]);
		assertEquals(null, v[9]);
		assertEquals(null, v[10]);
		assertEquals(null, v[11]);
		assertEquals(null, v[12]);
		assertEquals(null, v[13]);
		assertEquals(null, v[14]);
		assertEquals(null, v[15]);
		assertEquals(null, v[16]);
		assertEquals(null, v[17]);
		assertEquals(null, v[18]);
		assertEquals(null, v[19]);
		assertEquals(null, v[20]);
		assertEquals(Integer.valueOf(1000), v[21]);
		assertEquals(Integer.valueOf(2000), v[22]);
		assertEquals(Integer.valueOf(1500), v[23]);
	}

	private static class SimulatedSum implements OpenemsComponent {

		protected final LongReadChannel productionActiveEnergy;

		public SimulatedSum() {
			this.productionActiveEnergy = Sum.ChannelId.PRODUCTION_ACTIVE_ENERGY.doc().createChannelInstance(this,
					Sum.ChannelId.PRODUCTION_ACTIVE_ENERGY);
		}

		@Override
		public String id() {
			return OpenemsConstants.SUM_ID;
		}

		@Override
		public String alias() {
			return OpenemsConstants.SUM_ID;
		}

		@Override
		public boolean isEnabled() {
			return true;
		}

		@Override
		public ComponentContext getComponentContext() {
			return null;
		}

		@Override
		public Channel<?> _channel(String channelName) {
			return this.productionActiveEnergy;
		}

		@Override
		public Collection<Channel<?>> channels() {
			return null;
		}
	}
}
