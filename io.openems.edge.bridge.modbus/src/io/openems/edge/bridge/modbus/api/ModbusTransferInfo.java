package io.openems.edge.bridge.modbus.api;

import java.time.Instant;

/**
 * Contains informations about a transfer that happend on the bus. Can be a
 * request from OpenEMS or a response from another device.
 * 
 * @param time              When this frame was transmitted/received
 * @param communicationType Request or response type
 * @param unitId            Modbus Unit ID of the frame
 */
public record ModbusTransferInfo(Instant time, ModbusCommunicationType communicationType, int unitId) {

	public enum ModbusCommunicationType {
		REQUEST, RESPONSE
	}
}
