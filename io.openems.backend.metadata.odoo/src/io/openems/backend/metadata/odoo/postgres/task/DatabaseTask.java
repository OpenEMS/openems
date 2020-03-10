package io.openems.backend.metadata.odoo.postgres.task;

import java.sql.SQLException;

import io.openems.backend.metadata.odoo.postgres.MyConnection;

public interface DatabaseTask {
	public void execute(MyConnection connection) throws SQLException;
}