package io.openems.edge.timedata.mssql;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.timedata.api.Timedata;
import io.openems.shared.mssql.MssqlConnector;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Timedata.Mssql", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
)
public class TimedataMssqlImpl extends AbstractOpenemsComponent 
		implements TimedataMssql, Timedata, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(TimedataMssqlImpl.class);
	
	private MssqlConnector mssqlConnector = null;
	
	// Counts the number of Cycles till data is written to MS-SQL DB.
	private int cycleCount = 0;
	
	private Config config = null;

	public TimedataMssqlImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				TimedataMssql.ChannelId.values() //
		);
	}
	
	@Reference
	protected ComponentManager componentManager;
	
	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.mssqlConnector = new MssqlConnector(config.server(), config.dbname(), config.port(), 
				config.username(), config.password(), config.isPasswordProtected(), config.isReadOnly());
		
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		if (this.mssqlConnector != null) {
			this.mssqlConnector.deactivate();
		}
	}
	
	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.collectAndWriteChannelValues();
			break;
		}
	}
	
	protected synchronized void collectAndWriteChannelValues() {
		
		if (this.cycleCount >= this.config.noOfCycles()) {
			this.cycleCount = 0;  // resetting the cycle count value
		}
		
		String query = "Insert into test values(1, 2, 3)";
		//this.mssqlConnector.write(query);
		
		//Test read
		this.mssqlConnector.testRead();
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
