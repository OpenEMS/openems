package io.openems.backend.metadata.odoo.postgres.task;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseTask {
	public void execute(Connection connection) throws SQLException;
}