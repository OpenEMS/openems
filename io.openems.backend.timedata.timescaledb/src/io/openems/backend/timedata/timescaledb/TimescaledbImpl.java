package io.openems.backend.timedata.timescaledb;

import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.timedata.Timedata;
import io.openems.backend.timedata.timescaledb.internal.read.TimescaledbReadHandler;
import io.openems.backend.timedata.timescaledb.internal.write.TimescaledbWriteHandler;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.ThreadPoolUtils;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Timedata.TimescaleDB", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		immediate = true //
)
public class TimescaledbImpl extends AbstractOpenemsBackendComponent implements Timedata {

	private final Logger log = LoggerFactory.getLogger(TimescaledbImpl.class);

	private final Config config;
	private final TimescaledbWriteHandler timescaledbWriteHandler;
	private final TimescaledbReadHandler timescaledbReadHandler;

	private final ScheduledExecutorService debugLogExecutor = Executors.newSingleThreadScheduledExecutor();

	@Activate
	public TimescaledbImpl(@Reference Metadata metadata, Config config) throws SQLException {
		super("Timedata.TimescaleDB");
		this.config = config;

		this.logInfo(this.log, "Activate [" //
				+ config.user() + (config.password() != null ? ":xxx" : "") //
				+ "@" + config.host() + ":" + config.port() //
				+ "/" + config.database() //
				+ (config.isReadOnly() ? "|READ_ONLY_MODE" : "") //
				+ "]");

		var enableWriteChannelAddresses = Stream.of(config.enableWriteChannelAddresses()) //
				.filter(Objects::nonNull) //
				.map(c -> c.strip()) //
				.collect(Collectors.toUnmodifiableSet());

		this.timescaledbReadHandler = new TimescaledbReadHandler(config, enableWriteChannelAddresses);
		this.timescaledbWriteHandler = new TimescaledbWriteHandler(config, enableWriteChannelAddresses,
				// Schema is initialized
				schema -> this.timescaledbReadHandler.setSchema(schema) //
		);

		this.debugLogExecutor.scheduleWithFixedDelay(() -> {
			// Debug-Log
			this.log.info(new StringBuilder("[TimescaleDB] [monitor] ") //
					.append(this.timescaledbWriteHandler.debugLog()) //
					.toString());
		}, 10, 10, TimeUnit.SECONDS);

	}

	@Deactivate
	private void deactivate() {
		this.logInfo(this.log, "Deactivate");
		this.timescaledbWriteHandler.deactivate();
		this.timescaledbReadHandler.deactivate();
		ThreadPoolUtils.shutdownAndAwaitTermination(this.debugLogExecutor, 0);
	}

	@Override
	public void write(String edgeId, TreeBasedTable<Long, String, JsonElement> data) throws OpenemsException {
		this.timescaledbWriteHandler.write(edgeId, data);
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		return this.timescaledbReadHandler.queryHistoricData(edgeId, fromDate, toDate, channels, resolution);
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		return this.timescaledbReadHandler.queryHistoricEnergy(edgeId, fromDate, toDate, channels);
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		return this.timescaledbReadHandler.queryHistoricEnergyPerPeriod(edgeId, fromDate, toDate, channels, resolution);
	}

	@Override
	public String id() {
		return this.config.id();
	}

}
