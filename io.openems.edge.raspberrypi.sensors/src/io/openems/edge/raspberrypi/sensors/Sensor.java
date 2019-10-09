package io.openems.edge.raspberrypi.sensors;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.raspberrypi.circuitboard.api.adc.Adc;
import io.openems.edge.raspberrypi.sensors.temperaturesensor.TemperatureSensoric;
import io.openems.edge.raspberrypi.spi.SpiInitial;
import org.osgi.service.component.annotations.Reference;
import io.openems.edge.raspberrypi.circuitboard.CircuitBoard;

import javax.naming.ConfigurationException;


public abstract class Sensor extends AbstractOpenemsComponent implements OpenemsComponent, TemperatureSensoric {
	@Reference
	SpiInitial spiInitial;
	private final String id;
	private final String type;
	private final String circuitBoardId;
	private String versionId;
	private final int adcId;
	private final int pinPosition;
	private int indexAdcOfCircuitBoard;


	public Sensor(String id, String type, String circuitBoardId,
				  int adcId, int pinPosition, io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
				  io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		this.id = id;
		this.type = type;
		this.circuitBoardId = circuitBoardId;
		this.adcId = adcId;
		this.pinPosition = pinPosition;

	}


	protected void addToCircuitBoard() throws ConfigurationException {
		for (CircuitBoard consolinno: spiInitial.getCircuitBoards()) {
			if (consolinno.getType().equals(this.type) && consolinno.getCircuitBoardId().equals(this.circuitBoardId)) {
				if (this.adcId > consolinno.getMcpListViaId().size()) {
					throw new ConfigurationException("Wrong ADC Position given, max size is "
							+ consolinno.getMcpListViaId().size());
				} else {
					this.indexAdcOfCircuitBoard =consolinno.getMcpListViaId().get(this.adcId);
					Adc allocatePin = spiInitial.getAdcList().get(indexAdcOfCircuitBoard);
					if (allocatePin.getPins().get(this.pinPosition).isUsed()) {
						throw new ConfigurationException(
								"Wrong Pin, Pin already used by: "
										+ allocatePin.getPins().get(this.pinPosition).isUsed());
					} else {
						allocatePin.getPins().get(this.pinPosition).setUsedBy(this.id);

					}
				}

				consolinno.addToSensors(this);
				this.versionId=consolinno.getVersionId();
			}
		}
	}


	public String getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public String getCircuitBoardId() {
		return circuitBoardId;
	}

	public int getAdcId() {
		return adcId;
	}

	public int getPinPosition() {
		return pinPosition;
	}

	public int getIndexAdcOfCircuitBoard() {
		return indexAdcOfCircuitBoard;
	}

	public SpiInitial getSpiInitial() {
		return spiInitial;
	}

	public String getVersionId() {
		return versionId;
	}
}
