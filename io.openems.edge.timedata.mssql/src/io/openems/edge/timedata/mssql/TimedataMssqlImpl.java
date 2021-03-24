package io.openems.edge.timedata.mssql;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;

import com.google.gson.JsonElement;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Timedata.Mssql", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TimedataMssqlImpl extends AbstractOpenemsComponent implements TimedataMssql, Timedata, OpenemsComponent {

	private Config config = null;

	public TimedataMssqlImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				TimedataMssql.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		// Source:
		// https://docs.microsoft.com/de-de/sql/connect/jdbc/data-source-sample?view=sql-server-ver15
		SQLServerDataSource ds = new SQLServerDataSource();
		ds.setUser("<user>");
		ds.setPassword("<password>");
		ds.setServerName("<server>");
		ds.setPortNumber(1234);
		ds.setDatabaseName("AdventureWorks");

		try (Connection con = ds.getConnection();
				CallableStatement cstmt = con.prepareCall("{call dbo.uspGetEmployeeManagers(?)}");) {
			// Execute a stored procedure that returns some data.
			cstmt.setInt(1, 50);
			ResultSet rs = cstmt.executeQuery();

			// Iterate through the data in the result set and display it.
			while (rs.next()) {
				System.out.println("EMPLOYEE: " + rs.getString("LastName") + ", " + rs.getString("FirstName"));
				System.out.println(
						"MANAGER: " + rs.getString("ManagerLastName") + ", " + rs.getString("ManagerFirstName"));
				System.out.println();
			}
		}
		// Handle any errors that may have occurred.
		catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, int resolution)
			throws OpenemsNamedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, int resolution)
			throws OpenemsNamedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<Optional<Object>> getLatestValue(ChannelAddress channelAddress) {
		// TODO Auto-generated method stub
		return null;
	}
}
