package io.openems.backend.timedata.timescaledb.internal.write;

import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariDataSource;

import de.bytefish.pgbulkinsert.row.SimpleRowWriter;
import de.bytefish.pgbulkinsert.row.SimpleRowWriter.Table;
import de.bytefish.pgbulkinsert.util.PostgreSqlUtils;
import io.openems.backend.timedata.timescaledb.internal.Type;

public class WritePointsHandler implements Runnable {

	private final Logger log = LoggerFactory.getLogger(WritePointsHandler.class);

	private final HikariDataSource dataSource;
	private final Type type;
	private final List<Point> points;
	private final Table table;

	public WritePointsHandler(HikariDataSource dataSource, Type type, List<Point> points) {
		this.dataSource = dataSource;
		this.type = type;
		this.points = points;

		this.table = new SimpleRowWriter.Table(null, type.rawTableName, new String[] { //
				"time", //
				"edge_channel_id", //
				"value" //
		});
	}

	@Override
	public void run() {
		try (//
				var con = this.dataSource.getConnection();
				SimpleRowWriter writer = new SimpleRowWriter(this.table, PostgreSqlUtils.getPGConnection(con)) //
		) {
			for (var point : this.points) {
				writer.startRow(this.type.fillRow(point));
			}

		} catch (SQLException e) {
			// 'Expected errors', e.g. PostgreSQL server stopped
			// -> short error log
			this.log.error("Unable to write Points. " + e.getClass().getSimpleName() + ": " + e.getMessage());

		} catch (Exception e) {
			// 'Unexpected errors' -> long stacktrace
			this.log.error("Unable to write Points. " + e.getClass().getSimpleName() + ": " + e.getMessage());
			e.printStackTrace();

		}
	}

}
