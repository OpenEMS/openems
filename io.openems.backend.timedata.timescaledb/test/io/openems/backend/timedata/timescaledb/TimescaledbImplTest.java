package io.openems.backend.timedata.timescaledb;

import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import io.openems.backend.common.test.DummyMetadata;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;

public class TimescaledbImplTest {

	@Ignore
	@Test
	public void test() throws SQLException, InterruptedException, OpenemsNamedException {
		var metadata = new DummyMetadata();
		var config = MyConfig.create() //
				.setHost(Credentials.HOST) //
				.setPort(5432) //
				.setUser(Credentials.USER) //
				.setPassword(Credentials.PASSWORD) //
				.setDatabase(Credentials.DATABASE) //
				.setReadOnly(true) //
				.setPoolSize(10) //
				.build();
		TimedataTimescaleDb sut = new TimedataTimescaleDb(metadata, config);

		var zone = ZoneId.of("Europe/Berlin");
		var fromDate = ZonedDateTime.of(2022, 06, 23, 0, 0, 0, 0, zone);
		var toDate = ZonedDateTime.of(2022, 06, 24, 0, 0, 0, 0, zone);

		// queryHistoricData
		// var result = sut.queryHistoricData("fems888", fromDate, toDate, //
		// Set.of(new ChannelAddress("_sum", "EssSoc"), new ChannelAddress("_power",
		// "_PropertyStrategy")),
		// new Resolution(5, ChronoUnit.MINUTES));

		// queryHistoricEnergy
		// var result = sut.queryHistoricEnergy("fems888", fromDate, toDate, //
		// Set.of(new ChannelAddress("_sum", "GridSellActiveEnergy"),
		// new ChannelAddress("_sum", "GridBuyActiveEnergy")));

		// queryHistoricEnergyPerPeriod
		var result = sut
				.queryHistoricEnergyPerPeriod("fems888", fromDate, toDate,
						Set.of(new ChannelAddress("_sum", "GridSellActiveEnergy"),
								new ChannelAddress("_sum", "GridBuyActiveEnergy")),
						new Resolution(1, ChronoUnit.HOURS));

		result.forEach((time, map) -> {
			System.out.println(time + ": " + map);
		});

	}

}
