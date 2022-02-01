package io.openems.backend.metadata.odoo.postgres.task;

import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariDataSource;

public abstract class DatabaseTask {

	/**
	 * Execute the Task, making sure the Connection handle is released properly on
	 * exception.
	 *
	 * @param dataSource the {@link HikariDataSource}
	 * @throws SQLException on error
	 */
	public void execute(HikariDataSource dataSource) throws SQLException {
		try (var con = dataSource.getConnection()) {
			this._execute(con);
		}
	}

	protected abstract void _execute(Connection connection) throws SQLException;
}