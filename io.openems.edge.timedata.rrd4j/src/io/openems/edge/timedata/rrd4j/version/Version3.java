package io.openems.edge.timedata.rrd4j.version;

import static java.lang.Math.min;

import java.io.IOException;
import java.util.Arrays;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.rrd4j.core.DsDef;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;

import io.openems.edge.timedata.rrd4j.Rrd4jConstants;
import io.openems.edge.timedata.rrd4j.Rrd4jSupplier;

@Component(//
		scope = ServiceScope.SINGLETON, //
		property = { "version:Integer=3" }, //
		immediate = true //
)
public final class Version3 extends AbstractVersion implements Version {

	public static record StaticConfigurationConstants(//
			int numberOfRowsCumulatedValues, //
			int numberOfRowsAverageMinuteValues //
	) {

	}

	private final StaticConfigurationConstants constants;

	protected Version3(ComponentContext context, StaticConfigurationConstants constants) {
		super(context);
		this.constants = constants;
	}

	@Activate
	public Version3(ComponentContext context) {
		this(context, new StaticConfigurationConstants(//
				10_800, // 1 step (60 minutes), 10800 rows (450 days)
				129_600 // 1 step (5 minutes), 129600 rows (450 days)
		));
	}

	@Override
	public RrdDb createNewDb(//
			final CreateDatabaseConfig config //
	) throws IOException {
		final var channelDef = Rrd4jSupplier.getDsDefForChannel(config.channelUnit());
		final var isCumulated = config.channelUnit().isCumulated();
		var rrdDef = new RrdDef(//
				config.path(), //
				config.startTime(), //
				// Step in [s], default: 3600 = 1 hour or 5 minutes if not cumulated
				isCumulated ? 3600 : Rrd4jConstants.DEFAULT_STEP_SECONDS //
		);
		rrdDef.addDatasource(//
				new DsDef(Rrd4jConstants.createDefaultDatasourceNameOf(this), //
						channelDef.dsType(), //
						Rrd4jConstants.DEFAULT_HEARTBEAT_SECONDS, // Heartbeat in [s], default 300 = 5 minutes
						channelDef.minValue(), channelDef.maxValue()));

		// detailed recordings
		rrdDef.addArchive(channelDef.consolFun(), 0.5, 1, //
				isCumulated ? this.constants.numberOfRowsCumulatedValues()
						: this.constants.numberOfRowsAverageMinuteValues());

		return RrdDb.getBuilder() //
				.setBackendFactory(config.factory()) //
				.setPool(config.pool()) //
				.setRrdDef(rrdDef) //
				.build();
	}

	@Override
	public RrdDb migrate(//
			final RrdDb oldDb, //
			final CreateDatabaseConfig config //
	) throws IOException {
		// if channel unit not is cumulated copy 5 minute values into new db
		if (!config.channelUnit().isCumulated()) {
			final var oldArchive = oldDb.getArchive(0);

			RrdDb newDb = null;
			try {
				newDb = this.createNewDb(config.withStartTime(oldDb.getLastUpdateTime()));

				final var newRobin = newDb.getArchive(0).getRobin(0);
				final var oldRobin = oldArchive.getRobin(0);
				final var size = min(newRobin.getSize(), oldRobin.getSize());

				// get the last values of the old robin
				final var values = oldArchive.getRobin(0).getValues(oldRobin.getSize() - size, size);
				final var newValues = new double[newRobin.getSize()];
				Arrays.fill(newValues, Double.NaN);
				System.arraycopy(values, 0, newValues, newValues.length - values.length, values.length);
				newRobin.update(newValues);

				// copy state especially needed for last value
				final var newDs = newDb.getDatasource(0);
				final var oldDs = oldDb.getDatasource(0);
				// DS-Name needs to be the same for copying otherwise an exception will be
				// thrown
				newDs.setDsName(oldDs.getName());
				oldDs.copyStateTo(newDs);
				newDs.setDsName(Rrd4jConstants.createDefaultDatasourceNameOf(this));

				return newDb;
			} catch (Throwable e) {
				if (newDb != null) {
					newDb.close();
				}
				throw e;
			}
		}

		// ...otherwise only store hourly values
		final var hourlyArchive = oldDb.getArchive(1);
		final var dataRequest = oldDb.createFetchRequest(hourlyArchive.getConsolFun(), hourlyArchive.getStartTime(),
				hourlyArchive.getEndTime(), 3600);
		final var fetchedData = dataRequest.fetchData();

		RrdDb newDb = null;
		try {
			newDb = this.createNewDb(config.withStartTime(fetchedData.getLastTimestamp()));
			final var robin = newDb.getArchive(0).getRobin(0);
			final var values = fetchedData.getValues()[0];

			// size may be different because the next hour is partially available
			final var numberOfValues = this.constants.numberOfRowsCumulatedValues();
			final var copiedValues = new double[numberOfValues];
			Arrays.fill(copiedValues, Double.NaN);

			// shift existing data by minus one hour
			final var length = Math.min(numberOfValues, values.length);
			final var startIndex = Math.max(numberOfValues - length - 1, 0);
			System.arraycopy(values, 0, copiedValues, startIndex, length);

			// set last value
			final var indexOfLastValue = Math.min(startIndex + length, copiedValues.length - 1);
			copiedValues[indexOfLastValue] = oldDb.getDatasource(0).getLastValue();

			robin.setValues(copiedValues);

			// copy state especially needed for last value
			final var newDs = newDb.getDatasource(0);
			final var oldDs = oldDb.getDatasource(0);
			// DS-Name needs to be the same for copying otherwise an exception will be
			// thrown
			newDs.setDsName(oldDs.getName());
			oldDs.copyStateTo(newDs);
			newDs.setDsName(Rrd4jConstants.createDefaultDatasourceNameOf(this));

			return newDb;
		} catch (Throwable e) {
			if (newDb != null) {
				newDb.close();
			}
			throw e;
		}
	}

}
