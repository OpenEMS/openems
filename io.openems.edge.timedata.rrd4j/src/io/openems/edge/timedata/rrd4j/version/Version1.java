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
		property = { "version:Integer=1" }, //
		immediate = true //
)
public final class Version1 extends AbstractVersion implements Version {

	@Activate
	public Version1(ComponentContext context) {
		super(context);
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
		rrdDef.addArchive(channelDef.consolFun(), 0.5, 1, 8_928); // 1 step (5 minutes), 8928 rows (31 days)
		rrdDef.addArchive(channelDef.consolFun(), 0.5, 12, 8_016); // 12 steps (60 minutes), 8016 rows (334 days)

		final var db = RrdDb.getBuilder() //
				.setBackendFactory(config.factory()) //
				.setPool(config.pool()) //
				.setRrdDef(rrdDef) //
				.build();

		return db;
	}

	@Override
	public RrdDb migrate(RrdDb oldDb, CreateDatabaseConfig config) throws IOException {
		// First version no migration needed.
		return oldDb;
	}
}
