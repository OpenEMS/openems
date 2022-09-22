package io.openems.edge.bridge.sml.api;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.openmuc.jrxtx.SerialPort;
import org.openmuc.jsml.structures.SmlFile;
import org.openmuc.jsml.transport.Transport;

/**
 * This class can be used to read SML Messages over a serial interface
 */
public class ExtendendedSerialReceiver {
	private DataInputStream serialPortImportStream;
	private DataInputStream fixedInputStream = null;

	public ExtendendedSerialReceiver(SerialPort serialPort) throws IOException {
		this.serialPortImportStream = new DataInputStream(new BufferedInputStream(serialPort.getInputStream()));
	}

	public SmlFile getSMLFile() throws IOException {
		Transport transport = new Transport();

		SmlFile smlFile = null;
		if (null != this.fixedInputStream) {
			smlFile = transport.getSMLFile(this.serialPortImportStream);
		} else {
			smlFile = transport.getSMLFile(this.fixedInputStream);
		}
		return smlFile;
	}

	/**
	 * Allows to fix the Input Stream if a meter's SML is not matching the SML
	 * specification. Some meters (e.g. Holley meter were not implemented to
	 * specification (CRC or current time).
	 * 
	 * @param inputStream
	 */
	public void setFixedInputStream(DataInputStream inputStream) {
		this.fixedInputStream = inputStream;
	}

	public DataInputStream getInputStream() {
		return this.serialPortImportStream;
	}

	/**
	 * @deprecated use {@link #close()} instead
	 * @throws IOException if an I/O error occurs.
	 */

	@Deprecated
	public void closeStream() throws IOException {
		serialPortImportStream.close();
		if(null != this.fixedInputStream) {
			this.fixedInputStream.close();
		}
	}

	public void close() throws IOException {
		serialPortImportStream.close();
		if(null != this.fixedInputStream) {
			this.fixedInputStream.close();
		}
	}
}
