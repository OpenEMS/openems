package io.openems.edge.bridge.sml.api;

import io.openems.edge.common.channel.Channel;

import java.util.zip.DataFormatException;

import javax.xml.bind.DatatypeConverter;

public class ChannelRecord {

	private Channel<?> channel;
	public byte[] obisCode = null;

	/**
	 * In this case you will request secondary address values. eg. manufacturer,
	 * device id or meter type.
	 *
	 * @param channel the Channel
	 * @param int     the obisCode
	 * @throws Exception
	 */
	public ChannelRecord(Channel<?> channel, String obisCodeHex) throws Exception {
		this.channel = channel;
		if (obisCodeHex.startsWith("0x") || obisCodeHex.startsWith("0X")) {
			parseObisCode(obisCodeHex.toUpperCase().replace(" ", "").replace("0X", ""));
		} else {
			throw new DataFormatException(String.format(
					"Only Hex OBIS codes starting with 0x prefix are supported. OBIS String is %s", obisCodeHex));
		}
	}

	public Channel<?> getChannel() {
		return this.channel;
	}

	public void setChannelId(Channel<?> channel) {
		this.channel = channel;
	}

	public byte[] getObisCode() {
		return this.obisCode;
	}

	private void parseObisCode(String hexObisCode) {
		this.obisCode = DatatypeConverter.parseHexBinary(hexObisCode);
	}
}
