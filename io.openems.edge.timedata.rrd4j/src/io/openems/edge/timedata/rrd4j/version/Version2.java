package io.openems.edge.timedata.rrd4j.version;

import java.io.IOException;

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
		property = { "version:Integer=2" }, //
		immediate = true //
)
public final class Version2 extends AbstractVersion implements Version {

	public static record StaticConfigurationConstants(//
			int numberOfRowsMinute, //
			int numberOfRowsHour //
	) {

	}

	private final StaticConfigurationConstants contants;

	public Version2(ComponentContext context, StaticConfigurationConstants contants) {
		super(context);
		this.contants = contants;
	}

	@Activate
	public Version2(ComponentContext context) {
		this(context, new StaticConfigurationConstants(//
				8_928, // 1 step (5 minutes), 8928 rows (31 days)
				8_016 // 12 steps (60 minutes), 8016 rows (334 days)
		));
	}

	@Override
	public RrdDb createNewDb(//
			CreateDatabaseConfig config //
	) throws IOException {
		var channelDef = Rrd4jSupplier.getDsDefForChannel(config.channelUnit());
		var rrdDef = new RrdDef(//
				config.path(), //
				config.startTime(), //
				// Step in [s], default: 300 = 5 minutes
				Rrd4jConstants.DEFAULT_STEP_SECONDS);
		rrdDef.addDatasource(//
				new DsDef(Rrd4jConstants.DEFAULT_DATASOURCE_NAME, //
						channelDef.dsType(), //
						Rrd4jConstants.DEFAULT_HEARTBEAT_SECONDS, // Heartbeat in [s], default 300 = 5 minutes
						channelDef.minValue(), channelDef.maxValue()));

		// detailed recordings
		rrdDef.addArchive(channelDef.consolFun(), 0.5, 1, this.contants.numberOfRowsMinute());
		rrdDef.addArchive(channelDef.consolFun(), 0.5, 12, this.contants.numberOfRowsHour());

		return RrdDb.getBuilder() //
				.setBackendFactory(config.factory()) //
				.setPool(config.pool()) //
				.setRrdDef(rrdDef) //
				.build();
	}

	@Override
	public RrdDb migrate(RrdDb oldDb, CreateDatabaseConfig config) throws IOException {
		// Read data of last month
		var lastTimestamp = oldDb.getLastUpdateTime();
		var firstTimestamp = lastTimestamp - 60 /* minute */ * 60 /* hour */ * 24 /* day */ * 31;
		var fetchRequest = oldDb.createFetchRequest(oldDb.getArchive(0).getConsolFun(), firstTimestamp, lastTimestamp);
		var fetchData = fetchRequest.fetchData();
		final var values = Rrd4jSupplier.postProcessData(fetchData, Rrd4jConstants.DEFAULT_HEARTBEAT_SECONDS);
		if (fetchData.getTimestamps().length > 0) {
			firstTimestamp = fetchData.getTimestamps()[0];
		}

		var newDb = this.createNewDb(config.withStartTime(oldDb.getArchive(oldDb.getArcCount() - 1).getStartTime()));
		// Migrate data
		var sample = newDb.createSample();
		for (var i = 0; i < values.length; i++) {
			sample.setTime(firstTimestamp + i * Rrd4jConstants.DEFAULT_HEARTBEAT_SECONDS);
			sample.setValue(0, values[i]);
			sample.update();
		}
		return newDb;
	}
}
