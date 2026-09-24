package io.openems.edge.simulator.csvexporter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.CsvDataExporter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class CsvDataExporterImpl extends AbstractOpenemsComponent implements OpenemsComponent, Controller {

	private final Logger log = LoggerFactory.getLogger(CsvDataExporterImpl.class);
	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private final DateTimeFormatter fileFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

	@Reference
	private ComponentManager componentManager;

	private Config config;
	private List<ChannelAddress> channelAddresses;
	private String csvFilePath;
	private boolean headerWritten = false;

	/** Buffer to store data rows between CSV writes. */
	private final List<String> dataBuffer = new CopyOnWriteArrayList<>();

	/** Timestamp of last CSV write. */
	private ZonedDateTime lastWriteTime;

	public CsvDataExporterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		// Build channel addresses
		this.channelAddresses = buildChannelAddresses();

		// Initialize CSV file path
		try {
			initializeCsvFile();
			this.lastWriteTime = ZonedDateTime.now(this.componentManager.getClock());
			this.log.info("CSV Data Exporter activated - exporting {} channels every {} minutes to: {}",
					this.channelAddresses.size(), config.writeIntervalMinutes(), this.csvFilePath);
		} catch (IOException e) {
			this.log.error("Failed to initialize CSV file: {}", e.getMessage(), e);
		}
	}

	@Deactivate
	protected void deactivate() {
		// Write any remaining buffered data before shutdown
		if (!this.dataBuffer.isEmpty()) {
			this.log.info("Deactivating - writing {} remaining buffered rows to CSV", this.dataBuffer.size());
			flushBufferToCsv();
		}
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		if (!this.isEnabled() || this.csvFilePath == null) {
			return;
		}

		// Collect current data row into buffer
		collectDataRow();

		// Check if it's time to write to CSV
		var now = ZonedDateTime.now(this.componentManager.getClock());
		var minutesSinceLastWrite = java.time.Duration.between(this.lastWriteTime, now).toMinutes();

		if (minutesSinceLastWrite >= this.config.writeIntervalMinutes()) {
			this.log.info("Writing {} buffered rows to CSV", this.dataBuffer.size());
			flushBufferToCsv();
			this.lastWriteTime = now;
		}
	}

	/**
	 * Initialize CSV file path.
	 */
	private void initializeCsvFile() throws IOException {
		// Create output directory
		var outputPath = Paths.get(this.config.outputPath());
		if (!Files.exists(outputPath)) {
			Files.createDirectories(outputPath);
		}

		// Create CSV filename with timestamp
		var timestamp = ZonedDateTime.now().format(this.fileFormatter);
		this.csvFilePath = String.format("%s/realtime_export_%s.csv", this.config.outputPath(), timestamp);
	}

	/**
	 * Build list of channel addresses for export.
	 */
	private List<ChannelAddress> buildChannelAddresses() {
		List<ChannelAddress> channels = new ArrayList<>();

		// Add ESS channels
		if (!this.config.ess_id().isEmpty()) {
			channels.add(new ChannelAddress(this.config.ess_id(), "Soc"));
			channels.add(new ChannelAddress(this.config.ess_id(), "ActivePower"));
			channels.add(new ChannelAddress(this.config.ess_id(), "ReactivePower"));
		}

		// Add individual ESS active powers
		if (this.config.ess_ids() != null) {
			for (String essId : this.config.ess_ids()) {
				channels.add(new ChannelAddress(essId, "ActivePower"));
				channels.add(new ChannelAddress(essId, "ReactivePower"));
			}
		}

		// Add individual battery channels
		if (this.config.battery_ids() != null) {
			for (String batteryId : this.config.battery_ids()) {
				channels.add(new ChannelAddress(batteryId, "Soc"));
				channels.add(new ChannelAddress(batteryId, "Voltage"));
				channels.add(new ChannelAddress(batteryId, "Current"));

				channels.add(new ChannelAddress(batteryId, "AvgBatteryTemperature"));
				channels.add(new ChannelAddress(batteryId, "MinCellTemperature"));
				channels.add(new ChannelAddress(batteryId, "MaxCellTemperature"));

				channels.add(new ChannelAddress(batteryId, "LinkVoltage"));
				channels.add(new ChannelAddress(batteryId, "InternalVoltage"));
			}
		}

		// Add CSV Active Power Controller channels
		if (this.config.controller_csv_active_power_ids() != null) {
			for (String controllerId : this.config.controller_csv_active_power_ids()) {
				channels.add(new ChannelAddress(controllerId, "CurrentPower"));
			}
		}

		// Add CSV Frequency Controller channels
		if (this.config.controller_csv_frequency_ids() != null) {
			for (String controllerId : this.config.controller_csv_frequency_ids()) {
				channels.add(new ChannelAddress(controllerId, "Frequency"));
				channels.add(new ChannelAddress(controllerId, "FcrPower"));
				channels.add(new ChannelAddress(controllerId, "Ffr1Power"));
				channels.add(new ChannelAddress(controllerId, "RechargePower"));
				channels.add(new ChannelAddress(controllerId, "CalculatedPower"));
				channels.add(new ChannelAddress(controllerId, "TotalPower"));
			}
		}

		return channels;
	}

	/**
	 * Collect current channel values into a data row and add to buffer.
	 */
	private void collectDataRow() {
		StringBuilder row = new StringBuilder();

		// Timestamp
		var now = ZonedDateTime.now(this.componentManager.getClock());
		row.append(now.format(this.formatter));

		// Channel values
		for (ChannelAddress addr : this.channelAddresses) {
			row.append(",");
			try {
				Channel<?> channel = this.componentManager.getChannel(addr);
				var value = channel.value().get();
				if (value != null) {
					row.append(value);
				}
			} catch (Exception e) {
				// Channel not available, leave empty
			}
		}

		this.dataBuffer.add(row.toString());
	}

	/**
	 * Write buffered data to CSV file and clear buffer.
	 */
	private void flushBufferToCsv() {
		if (this.dataBuffer.isEmpty()) {
			return;
		}

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.csvFilePath, true))) {
			// Write header if not yet written
			if (!this.headerWritten) {
				writeHeader(writer);
				this.headerWritten = true;
			}

			// Write all buffered rows
			for (String row : this.dataBuffer) {
				writer.write(row);
				writer.newLine();
			}

			writer.flush();
			this.log.debug("Flushed {} rows to CSV", this.dataBuffer.size());

			// Clear buffer after successful write
			this.dataBuffer.clear();

		} catch (IOException e) {
			this.log.error("Error writing to CSV file: {}", e.getMessage(), e);
		}
	}

	/**
	 * Write CSV header row with units.
	 */
	private void writeHeader(BufferedWriter writer) throws IOException {
		StringBuilder header = new StringBuilder();
		header.append("timestamp");

		for (ChannelAddress addr : this.channelAddresses) {
			header.append(",");
			try {
				Channel<?> channel = this.componentManager.getChannel(addr);
				var unit = channel.channelDoc().getUnit();

				header.append(addr.toString()).append(" [").append(unit.symbol).append("]");
			} catch (Exception e) {
				header.append(addr.toString());
			}
		}

		writer.write(header.toString());
		writer.newLine();
	}
}
