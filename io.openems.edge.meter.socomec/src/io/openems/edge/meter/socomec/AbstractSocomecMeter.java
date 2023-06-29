package io.openems.edge.meter.socomec;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.ModbusUtils;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;

public abstract class AbstractSocomecMeter extends AbstractOpenemsModbusComponent
		implements SocomecMeter, ElectricityMeter, OpenemsComponent, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(AbstractSocomecMeter.class);

	protected final ModbusProtocol modbusProtocol;

	protected AbstractSocomecMeter(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) throws OpenemsException {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		this.modbusProtocol = new ModbusProtocol(this);
	}

	@Override
	protected final ModbusProtocol defineModbusProtocol() {
		return this.modbusProtocol;
	}

	/**
	 * Applies the modbus protocol for Socomec Countis E23, E24, E27 and E28. All
	 * are identical.
	 *
	 * @throws OpenemsException on error
	 */
	protected abstract void identifiedCountisE23_E24_E27_E28() throws OpenemsException;

	/**
	 * Applies the modbus protocol for Socomec Countis E34, E44.
	 *
	 * @throws OpenemsException on error
	 */
	protected abstract void identifiedCountisE34_E44() throws OpenemsException;

	/**
	 * Applies the modbus protocol for Socomec Diris A10.
	 *
	 * @throws OpenemsException on error
	 */
	protected abstract void identifiedDirisA10() throws OpenemsException;

	/**
	 * Applies the modbus protocol for Socomec Diris A14.
	 *
	 * @throws OpenemsException on error
	 */
	protected abstract void identifiedDirisA14() throws OpenemsException;

	/**
	 * Applies the modbus protocol for Socomec Diris B30.
	 *
	 * @throws OpenemsException on error
	 */
	protected abstract void identifiedDirisB30() throws OpenemsException;

	/**
	 * Applies the modbus protocol for Socomec Countis E14.
	 *
	 * @throws OpenemsException on error
	 */
	protected abstract void identifiedCountisE14() throws OpenemsException;

	protected final void identifySocomecMeter() {
		this.getSocomecIdentifier().thenAccept(name -> {
			try {

				if (name.startsWith("countis e23")) {
					this.logInfo(this.log, "Identified Socomec Countis E23 meter");
					this.identifiedCountisE23_E24_E27_E28();

				} else if (name.startsWith("countis e24")) {
					this.logInfo(this.log, "Identified Socomec Countis E24 meter");
					this.identifiedCountisE23_E24_E27_E28();

				} else if (name.startsWith("countis e27")) {
					this.logInfo(this.log, "Identified Socomec Countis E27 meter");
					this.identifiedCountisE23_E24_E27_E28();

				} else if (name.startsWith("countis e28")) {
					this.logInfo(this.log, "Identified Socomec Countis E28 meter");
					this.identifiedCountisE23_E24_E27_E28();

				} else if (name.startsWith("countis e34")) {
					this.logInfo(this.log, "Identified Socomec Countis E34 meter");
					this.identifiedCountisE34_E44();

				} else if (name.startsWith("countis e44")) {
					this.logInfo(this.log, "Identified Socomec Countis E44 meter");
					this.identifiedCountisE34_E44();

				} else if (name.startsWith("diris a-10") || name.startsWith("diris a10")) {
					this.logInfo(this.log, "Identified Socomec Diris A10 meter");
					this.identifiedDirisA10();

				} else if (name.startsWith("diris a14")) {
					this.logInfo(this.log, "Identified Socomec Diris A14 meter");
					this.identifiedDirisA14();

				} else if (name.startsWith("diris b30")) {
					this.logInfo(this.log, "Identified Socomec Diris B30 meter");
					this.identifiedDirisB30();

				} else if (name.startsWith("countis e14")) {
					this.logError(this.log, "Identified Socomec [" + name + "] meter");
					this.identifiedCountisE14();

				} else {
					this.logError(this.log, "Unable to identify Socomec [" + name + "] meter!");
					this.channel(SocomecMeter.ChannelId.UNKNOWN_SOCOMEC_METER).setNextValue(true);
				}

			} catch (OpenemsException e) {
				this.channel(SocomecMeter.ChannelId.UNKNOWN_SOCOMEC_METER).setNextValue(true);
				this.logError(this.log,
						"Error whily trying to identify Socomec [" + name + "] meter: " + e.getMessage());
				e.printStackTrace();
			}
		});
	}

	/**
	 * Gets the SOCOMEC identifier via Modbus.
	 *
	 * @return the future String; returns an empty string on error, never an
	 *         exception;
	 */
	private CompletableFuture<String> getSocomecIdentifier() {
		// Prepare result
		final var result = new CompletableFuture<String>();

		// Search for Socomec identifier register. Needs to be "SOCO".
		try {
			ModbusUtils.readELementOnce(this.modbusProtocol, new UnsignedQuadruplewordElement(0xC350), true)
					.thenAccept(value -> {
						if (value != 0x0053004F0043004FL /* SOCO */) {
							this.channel(SocomecMeter.ChannelId.NO_SOCOMEC_METER).setNextValue(true);
							return;
						}
						// Found Socomec meter
						try {
							ModbusUtils.readELementOnce(this.modbusProtocol, new StringWordElement(0xC38A, 8), true)
									.thenAccept(name -> {
										result.complete(name.toLowerCase());
									});

						} catch (OpenemsException e) {
							this.logWarn(this.log, "Error while trying to identify Socomec meter: " + e.getMessage());
							e.printStackTrace();
							result.complete("");
						}
					});

		} catch (OpenemsException e) {
			this.logWarn(this.log, "Error while trying to identify Socomec meter: " + e.getMessage());
			e.printStackTrace();
			result.complete("");
		}

		return result;
	}

}
