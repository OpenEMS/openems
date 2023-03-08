package io.openems.shared.influxdb.proxy;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;

public class QueryBuilderTest {

	private static final String BUCKET = "db/default";
	private static final String MEASUREMENT = "data";
	private static final Optional<Integer> EDGE_ID = Optional.of(888);
	private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
	private static final ZonedDateTime FROM_DATE = ZonedDateTime.of(2022, 12, 29, 0, 0, 0, 0, ZONE);
	private static final ZonedDateTime TO_DATE = ZonedDateTime.of(2022, 12, 29, 9, 0, 0, 0, ZONE);
	private static final Set<ChannelAddress> POWER_CHANNELS = Set.of(new ChannelAddress("_sum", "EssActivePower"),
			new ChannelAddress("_sum", "GridActivePower"));
	private static final Set<ChannelAddress> ENERGY_CHANNELS = Set.of(
			new ChannelAddress("_sum", "EssActiveDischargeEnergy"),
			new ChannelAddress("_sum", "EssActiveChargeEnergy"));
	private static final Resolution RESOLUTION = new Resolution(5, ChronoUnit.MINUTES);

	private static final QueryProxy FLUX = QueryProxy.flux();
	private static final QueryProxy INFLUX_QL = QueryProxy.influxQl();

	@Test
	public void testFluxBuildHistoricDataQuery() throws OpenemsNamedException {
		FLUX.buildHistoricDataQuery(BUCKET, MEASUREMENT, EDGE_ID, FROM_DATE, TO_DATE, POWER_CHANNELS, RESOLUTION);
	}

	@Test
	public void testInfluxqlBuildHistoricDataQuery() throws OpenemsNamedException {
		INFLUX_QL.buildHistoricDataQuery(BUCKET, MEASUREMENT, EDGE_ID, FROM_DATE, TO_DATE, POWER_CHANNELS, RESOLUTION);
	}

	@Test
	public void testFluxBuildHistoricEnergyQuery() throws OpenemsNamedException {
		FLUX.buildHistoricEnergyQuery(BUCKET, MEASUREMENT, EDGE_ID, FROM_DATE, TO_DATE, ENERGY_CHANNELS);
	}

	@Test
	public void testInfluxqlBuildHistoricEnergyQuery() throws OpenemsNamedException {
		INFLUX_QL.buildHistoricEnergyQuery(BUCKET, MEASUREMENT, EDGE_ID, FROM_DATE, TO_DATE, ENERGY_CHANNELS);
	}

	@Test
	public void testFluxBuildHistoricEnergyQueryPerPeriod() throws OpenemsNamedException {
		FLUX.buildHistoricEnergyPerPeriodQuery(BUCKET, MEASUREMENT, EDGE_ID, FROM_DATE, TO_DATE, ENERGY_CHANNELS,
				RESOLUTION);
	}

	@Test
	public void testInfluxqlBuildHistoricEnergyPerPeriodQuery() throws OpenemsNamedException {
		INFLUX_QL.buildHistoricEnergyPerPeriodQuery(BUCKET, MEASUREMENT, EDGE_ID, FROM_DATE, TO_DATE, ENERGY_CHANNELS,
				RESOLUTION);
	}

	@Test
	public void testInlfuxqlFetchAvailableSinceQuery() throws OpenemsNamedException {
		INFLUX_QL.buildFetchAvailableSinceQuery(BUCKET);
	}

	@Test
	public void testFluxFetchAvailableSinceQuery() throws OpenemsNamedException {
		FLUX.buildFetchAvailableSinceQuery(BUCKET);
	}

}
