package io.openems.edge.predictor.production.linearmodel.prediction;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.predictor.api.common.PredictionException;
import io.openems.edge.predictor.production.linearmodel.prediction.SnowStateMachine.State;
import io.openems.edge.timedata.test.DummyTimedata;
import io.openems.edge.weather.api.QuarterlyWeatherSnapshot;
import io.openems.edge.weather.api.Weather;

@RunWith(MockitoJUnitRunner.class)
public class SnowStateMachineTest {

	private static final ChannelAddress PRODUCTION_CHANNEL_ADDRESS = new ChannelAddress("_sum", "ProductionChannel");

	private final Clock clock = Clock.fixed(Instant.parse("2025-10-28T12:00:00Z"), ZoneId.of("Europe/Berlin"));

	@Mock
	private Weather weather;

	@Mock
	private Supplier<Integer> maxProductionSupplier;

	private SnowStateMachine sut;
	private DummyTimedata timedata;

	@Before
	public void setUp() {
		this.timedata = new DummyTimedata("timedata0");
		this.sut = new SnowStateMachine(//
				this.weather, //
				this.timedata, //
				() -> this.clock, //
				PRODUCTION_CHANNEL_ADDRESS, //
				this.maxProductionSupplier, //
				State.NORMAL);
	}

	@Test
	public void testRun_ShouldSwitchToSnow_WhenSnowIsFallingAndSnowWillStay() throws Exception {
		var snowfalls = List.of(1., 0., 0., 0., 0., 0., 0., 0., 0., 0.);
		var snowDepths = List.of(1., 1., 1., 1., 0., 1., 1., 1., 1., 1.);
		var weatherData = this.buildWeatherData(snowfalls, snowDepths);
		when(this.weather.getQuarterlyWeatherForecast(anyInt())).thenReturn(weatherData);

		this.sut.setCurrentState(State.NORMAL);
		this.sut.run();

		assertEquals(State.SNOW, this.sut.getCurrentState());
		assertEquals(roundDownToQuarter(ZonedDateTime.now(this.clock)), this.sut.getSnowStart());
	}

	@Test
	public void testRun_ShouldNotSwitchToSnow_WhenSnowIsFallingButSnowWillNotStay() throws Exception {
		var snowfalls = List.of(1., 0., 0., 0., 0., 0., 0., 0., 0., 0.);
		var snowDepths = List.of(1., 1., 1., 0., 0., 1., 1., 1., 1., 1.);
		var weatherData = this.buildWeatherData(snowfalls, snowDepths);
		when(this.weather.getQuarterlyWeatherForecast(anyInt())).thenReturn(weatherData);

		this.sut.setCurrentState(State.NORMAL);
		this.sut.run();

		assertEquals(State.NORMAL, this.sut.getCurrentState());
		assertNull(this.sut.getSnowStart());
	}

	@Test
	public void testRun_ShouldNotSwitchToSnow_WhenSnowIsNotFallingButSnowWillStay() throws Exception {
		var snowfalls = List.of(0., 1., 0., 0., 0., 0., 0., 0., 0., 0.);
		var snowDepths = List.of(1., 1., 1., 1., 0., 1., 1., 1., 1., 1.);
		var weatherData = this.buildWeatherData(snowfalls, snowDepths);
		when(this.weather.getQuarterlyWeatherForecast(anyInt())).thenReturn(weatherData);

		this.sut.setCurrentState(State.NORMAL);
		this.sut.run();

		assertEquals(State.NORMAL, this.sut.getCurrentState());
		assertNull(this.sut.getSnowStart());
	}

	@Test
	public void testRun_ShouldSwitchToNormal_WhenNoSnowOnGroundAndSnowWillNotStay() throws Exception {
		var snowfalls = List.of(0., 0., 0., 0., 0., 0., 0., 0., 0., 0.);
		var snowDepths = List.of(0., 1., 1., 1., 0., 1., 1., 1., 1., 1.);
		var weatherData = this.buildWeatherData(snowfalls, snowDepths);
		when(this.weather.getQuarterlyWeatherForecast(anyInt())).thenReturn(weatherData);

		this.sut.setCurrentState(State.SNOW);
		var snowStart = roundDownToQuarter(ZonedDateTime.now(this.clock).minus(Duration.ofDays(2)));
		this.sut.setSnowStart(snowStart);
		this.sut.run();

		assertEquals(State.NORMAL, this.sut.getCurrentState());
		assertNull(this.sut.getSnowStart());
	}

	@Test
	public void testRun_ShouldNotSwitchToNormal_WhenStillSnowOnGroundButSnowWillNotStay() throws Exception {
		var snowfalls = List.of(0., 0., 0., 0., 0., 0., 0., 0., 0., 0.);
		var snowDepths = List.of(1., 1., 1., 0., 0., 1., 1., 1., 1., 1.);
		var weatherData = this.buildWeatherData(snowfalls, snowDepths);
		when(this.weather.getQuarterlyWeatherForecast(anyInt())).thenReturn(weatherData);
		when(this.maxProductionSupplier.get()).thenReturn(Integer.MAX_VALUE);

		this.sut.setCurrentState(State.SNOW);
		var snowStart = roundDownToQuarter(ZonedDateTime.now(this.clock).minus(Duration.ofDays(2)));
		this.sut.setSnowStart(snowStart);
		this.sut.run();

		assertEquals(State.SNOW, this.sut.getCurrentState());
		assertEquals(snowStart, this.sut.getSnowStart());
	}

	@Test
	public void testRun_ShouldNotSwitchToNormal_WhenNoSnowOnGroundButSnowWillStay() throws Exception {
		var snowfalls = List.of(0., 0., 0., 0., 0., 0., 0., 0., 0., 0.);
		var snowDepths = List.of(0., 1., 1., 1., 1., 1., 1., 1., 1., 1.);
		var weatherData = this.buildWeatherData(snowfalls, snowDepths);
		when(this.weather.getQuarterlyWeatherForecast(anyInt())).thenReturn(weatherData);
		when(this.maxProductionSupplier.get()).thenReturn(Integer.MAX_VALUE);

		this.sut.setCurrentState(State.SNOW);
		var snowStart = roundDownToQuarter(ZonedDateTime.now(this.clock).minus(Duration.ofDays(2)));
		this.sut.setSnowStart(snowStart);
		this.sut.run();

		assertEquals(State.SNOW, this.sut.getCurrentState());
		assertEquals(snowStart, this.sut.getSnowStart());
	}

	@Test
	public void testRun_ShouldSwitchToNormal_WhenSnowAtLeast24hAndStableHighProduction() throws Exception {
		var snowfalls = List.of(0., 0., 0., 0., 0., 0., 0., 0., 0., 0.);
		var snowDepths = List.of(1., 1., 1., 1., 1., 1., 1., 1., 1., 1.);
		var weatherData = this.buildWeatherData(snowfalls, snowDepths);
		when(this.weather.getQuarterlyWeatherForecast(anyInt())).thenReturn(weatherData);

		var latestProductionValues = List.of(3, 3, 3, 3, 2, 2);
		for (int i = 1; i <= 6; i++) {
			this.timedata.add(//
					ZonedDateTime.now(this.clock).minusMinutes(i * 15), //
					PRODUCTION_CHANNEL_ADDRESS, //
					latestProductionValues.get(i - 1));
		}

		when(this.maxProductionSupplier.get()).thenReturn(10);

		this.sut.setCurrentState(State.SNOW);
		var snowStart = roundDownToQuarter(ZonedDateTime.now(this.clock).minus(Duration.ofDays(2)));
		this.sut.setSnowStart(snowStart);
		this.sut.run();

		assertEquals(State.NORMAL, this.sut.getCurrentState());
		assertNull(this.sut.getSnowStart());
	}

	@Test
	public void testRun_ShouldNotSwitchToNormal_WhenSnowAtLeast24hButNoStableHighProduction() throws Exception {
		var snowfalls = List.of(0., 0., 0., 0., 0., 0., 0., 0., 0., 0.);
		var snowDepths = List.of(1., 1., 1., 1., 1., 1., 1., 1., 1., 1.);
		var weatherData = this.buildWeatherData(snowfalls, snowDepths);
		when(this.weather.getQuarterlyWeatherForecast(anyInt())).thenReturn(weatherData);

		var latestProductionValues = List.of(3, 3, 3, 2, 2, 2);
		for (int i = 1; i <= 6; i++) {
			this.timedata.add(//
					ZonedDateTime.now(this.clock).minusMinutes(i * 15), //
					PRODUCTION_CHANNEL_ADDRESS, //
					latestProductionValues.get(i - 1));
		}

		when(this.maxProductionSupplier.get()).thenReturn(10);

		this.sut.setCurrentState(State.SNOW);
		var snowStart = roundDownToQuarter(ZonedDateTime.now(this.clock).minus(Duration.ofDays(2)));
		this.sut.setSnowStart(snowStart);
		this.sut.run();

		assertEquals(State.SNOW, this.sut.getCurrentState());
		assertEquals(snowStart, this.sut.getSnowStart());
	}

	@Test
	public void testRun_ShouldNotSwitchToNormal_WhenSnowNotAtLeast24hButStableHighProduction() throws Exception {
		var snowfalls = List.of(0., 0., 0., 0., 0., 0., 0., 0., 0., 0.);
		var snowDepths = List.of(1., 1., 1., 1., 1., 1., 1., 1., 1., 1.);
		var weatherData = this.buildWeatherData(snowfalls, snowDepths);
		when(this.weather.getQuarterlyWeatherForecast(anyInt())).thenReturn(weatherData);

		var latestProductionValues = List.of(3, 3, 3, 3, 2, 2);
		for (int i = 1; i <= 6; i++) {
			this.timedata.add(//
					ZonedDateTime.now(this.clock).minusMinutes(i * 15), //
					PRODUCTION_CHANNEL_ADDRESS, //
					latestProductionValues.get(i - 1));
		}

		this.sut.setCurrentState(State.SNOW);
		var snowStart = roundDownToQuarter(ZonedDateTime.now(this.clock).minus(Duration.ofHours(23)));
		this.sut.setSnowStart(snowStart);
		this.sut.run();

		assertEquals(State.SNOW, this.sut.getCurrentState());
		assertEquals(snowStart, this.sut.getSnowStart());
	}

	@Test
	public void testRun_ShouldThrowException_WhenNoWeatherData() throws Exception {
		when(this.weather.getQuarterlyWeatherForecast(anyInt())).thenThrow(OpenemsException.class);

		var exception = assertThrows(PredictionException.class, () -> {
			this.sut.run();
		});
		assertEquals(PredictionError.NO_WEATHER_DATA, exception.getError());
	}

	private List<QuarterlyWeatherSnapshot> buildWeatherData(List<Double> snowfalls, List<Double> snowDepths) {
		int size = Math.min(snowfalls.size(), snowDepths.size());
		var result = new ArrayList<QuarterlyWeatherSnapshot>();

		for (int i = 0; i < size; i++) {
			result.add(new QuarterlyWeatherSnapshot(//
					ZonedDateTime.now(this.clock).plusMinutes(i * 15), //
					0.0, // shortwaveRadiation
					0.0, // directRadiation
					0.0, // directNormalIrradiance
					0.0, // diffuseRadiation
					0.0, // temperature
					snowfalls.get(i), //
					snowDepths.get(i)));
		}

		return result;
	}
}
