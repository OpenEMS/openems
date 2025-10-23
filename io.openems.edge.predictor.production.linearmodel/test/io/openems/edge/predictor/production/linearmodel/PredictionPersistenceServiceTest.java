package io.openems.edge.predictor.production.linearmodel;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.production.linearmodel.PredictionPersistenceService.ChannelMapping;
import io.openems.edge.timedata.test.DummyTimedata;

@RunWith(MockitoJUnitRunner.class)
public class PredictionPersistenceServiceTest {

	private static ChannelAddress CHANNEL_1H_AHEAD = new ChannelAddress("predictor0", "Channel1hAhead");
	private static ChannelAddress CHANNEL_6H_AHEAD = new ChannelAddress("predictor0", "Channel6hAhead");

	@Mock
	private PredictorProductionLinearModel parent;

	private List<ChannelMapping> channelMappings;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() {
		var channel1hAhead = mock(Channel.class);
		var channel6hAhead = mock(Channel.class);

		when(this.parent.getPrediction1hAheadChannel()).thenReturn(channel1hAhead);
		when(channel1hAhead.address()).thenReturn(CHANNEL_1H_AHEAD);

		when(this.parent.getPrediction6hAheadChannel()).thenReturn(channel6hAhead);
		when(channel6hAhead.address()).thenReturn(CHANNEL_6H_AHEAD);

		this.channelMappings = List.of(//
				new ChannelMapping(//
						1, //
						this.parent.getPrediction1hAheadChannel().address(), //
						this.parent::_setPrediction1hAhead, //
						this.parent::_setPrediction1hRealized), //
				new ChannelMapping(//
						6, //
						this.parent.getPrediction6hAheadChannel().address(), //
						this.parent::_setPrediction6hAhead, //
						this.parent::_setPrediction6hRealized));
	}

	@Test
	public void testShiftPredictionToRealizedChannel_ShouldSetRealizedValues() {
		var clock = new TimeLeapClock();
		var now = roundDownToQuarter(ZonedDateTime.now(clock));

		var timedata = new DummyTimedata("timedata0");
		timedata.add(now.minusHours(1), CHANNEL_1H_AHEAD, 10);
		timedata.add(now.minusHours(6), CHANNEL_6H_AHEAD, 60);

		var sut = new PredictionPersistenceService(//
				timedata, //
				() -> clock, //
				this.channelMappings);

		sut.shiftPredictionToRealizedChannel();

		verify(this.parent)._setPrediction1hRealized(eq(10));
		verify(this.parent)._setPrediction6hRealized(eq(60));
	}

	@Test
	public void testUpdatePredictionAheadChannels_ShouldSetPredictionAheadValues() {
		var clock = new TimeLeapClock();
		var now = roundDownToQuarter(ZonedDateTime.now(clock));

		Integer[] values = new Integer[6 /* hours */ * 4 /* quarters */ + 1 /* buffer */];
		for (int i = 0; i < values.length; i++) {
			values[i] = i * 10;
		}
		var prediction = Prediction.from(now, values);

		var sut = new PredictionPersistenceService(//
				new DummyTimedata("timedata0"), //
				() -> clock, //
				this.channelMappings);

		sut.updatePredictionAheadChannels(prediction);

		verify(this.parent)._setPrediction1hAhead(eq(40));
		verify(this.parent)._setPrediction6hAhead(eq(240));
	}
}
