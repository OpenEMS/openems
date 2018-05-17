package io.openems.edge.timedata.influxdb;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.influx4j.InfluxDB;
import com.zaxxer.influx4j.Point;
import com.zaxxer.influx4j.PointFactory;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.timedata.influxdb.InfluxTimedata.ChannelId;

public class InfluxWrite {

	private final Logger log = LoggerFactory.getLogger(InfluxWrite.class);

	private final InfluxTimedata parent;

	private InfluxDB _influxDB = null;
	private PointFactory pointFactory = null;
	private int pointFactoryMaxSize = 0;

	public InfluxWrite(InfluxTimedata parent) {
		this.parent = parent;
	}

	/**
	 * Get InfluxDB Connection
	 * 
	 * @return
	 */
	protected Optional<InfluxDB> getConnection() {
		if (this._influxDB == null) {
			try {
				com.zaxxer.influx4j.InfluxDB influxDB = com.zaxxer.influx4j.InfluxDB.builder() //
						.setConnection(this.parent.ip, this.parent.port, com.zaxxer.influx4j.InfluxDB.Protocol.HTTP) //
						.setUsername(this.parent.username) //
						.setPassword(this.parent.password) //
						.setDatabase(this.parent.database) //
						.build();
				influxDB.createDatabase(this.parent.database);
				this.parent.channel(ChannelId.STATE_0).setNextValue(false);
				this._influxDB = influxDB;
			} catch (RuntimeException e) {
				this.log.warn("Unable to connect to InfluxDB (write): " + e.getMessage());
				this.parent.channel(ChannelId.STATE_0).setNextValue(true);
			}
		}
		return Optional.ofNullable(this._influxDB);
	}

	protected synchronized void updatePointFactory() {
		int noOfChannels = 0;
		for (OpenemsComponent component : this.parent._components) {
			noOfChannels += component.channels().size();
		}
		int calculatedMaxSize = Math.round(noOfChannels * 1.1f) + 1;
		if (this.pointFactory == null || this.pointFactoryMaxSize < calculatedMaxSize
				|| this.pointFactoryMaxSize > calculatedMaxSize * 1.1f) {
			// create a new PointFactory
			this.pointFactory = PointFactory.builder() //
					.initialSize(calculatedMaxSize) //
					.maximumSize(calculatedMaxSize) //
					.build();
			this.pointFactoryMaxSize = calculatedMaxSize;
		}
	}

	protected synchronized void collectAndWriteChannelValues() {
		Optional<InfluxDB> connectionOpt = this.getConnection();
		if (!connectionOpt.isPresent()) {
			this.log.warn("Connection not available. Not perisisting any data!");
			return;
		}
		InfluxDB connection = connectionOpt.get();
		long timestamp = System.currentTimeMillis() / 1000;
		final Point point = this.pointFactory //
				.createPoint(InfluxTimedata.MEASUREMENT) //
				.timestamp(timestamp, TimeUnit.SECONDS);
		this.parent._components.stream().filter(c -> c.isEnabled()).forEach(component -> {
			component.channels().forEach(channel -> {
				Optional<?> valueOpt = channel.value().asOptional();
				if (!valueOpt.isPresent()) {
					// ignore not available channels
					return;
				}
				Object value = valueOpt.get();
				String address = channel.address().toString();
				try {
					switch (channel.getType()) {
					case BOOLEAN:
						point.field(address, (Boolean) value);
						break;
					case FLOAT:
						point.field(address, (Float) value);
						break;
					case INTEGER:
						point.field(address, (Integer) value);
						break;
					case LONG:
						point.field(address, (Long) value);
						break;
					case SHORT:
						point.field(address, (Short) value);
						break;
					case STRING:
						point.field(address, (String) value);
						break;
					}
				} catch (IllegalArgumentException e) {
					this.log.warn("Unable to add Channel [" + address + "] value [" + value + "]: " + e.getMessage());
				}
			});
		});
		connection.write(point);
	}
}
