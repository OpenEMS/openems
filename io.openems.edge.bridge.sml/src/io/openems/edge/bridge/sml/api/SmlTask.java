package io.openems.edge.bridge.sml.api;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;

import javax.xml.bind.DatatypeConverter;

import org.openmuc.jsml.structures.SmlFile;
import org.openmuc.jsml.transport.SerialReceiver;

public class SmlTask {

	private final AbstractOpenemsSmlComponent openemsSmlComponent; // creator of this task instance
	private SerialReceiver serialReceiver;
	private BridgeSml bridgeSml;
	private InputStreamFixupInterface fixupInterface = null;
	private ExtendendedSerialReceiver fixedSerialReceiver = null;

	public SmlTask(BridgeSml bridgeSml, AbstractOpenemsSmlComponent openemsSmlComponent) {
		this.openemsSmlComponent = openemsSmlComponent;
		this.bridgeSml = bridgeSml;
		try {
			this.serialReceiver = new SerialReceiver(this.bridgeSml.getSmlConnection());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public SmlTask(BridgeSml bridgeSml, AbstractOpenemsSmlComponent openemsSmlComponent,
			InputStreamFixupInterface fixupInterface) {
		this.openemsSmlComponent = openemsSmlComponent;
		this.bridgeSml = bridgeSml;
		try {
			this.fixedSerialReceiver = new ExtendendedSerialReceiver(this.bridgeSml.getSmlConnection());
			this.fixupInterface = fixupInterface;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Get the Request.
	 * 
	 * @return a {@link VariableDataStructure}
	 * @throws InterruptedIOException on error
	 * @throws IOException            on error
	 */
	public SmlFile getRequest() throws InterruptedIOException, IOException {
		SmlFile file;
		if ((null != this.fixedSerialReceiver) && (null != this.fixupInterface)) {
			var fixedStream = this.fixupInterface.fixInputStream(
					DatatypeConverter.printBase64Binary(this.fixedSerialReceiver.getInputStream().readAllBytes()));
			DataInputStream newInputStream = new DataInputStream(
					new ByteArrayInputStream(DatatypeConverter.parseHexBinary(fixedStream)));
			this.fixedSerialReceiver.setFixedInputStream(newInputStream);
			file = this.fixedSerialReceiver.getSMLFile();
		} else

		{
			file = this.serialReceiver.getSMLFile();
		}
		return file;
	}

	public void setResponse(SmlFile data) {
		new ChannelDataRecordMapper(data, this.openemsSmlComponent.getChannelDataRecordsList());
	}

	public void setFixupInterface(InputStreamFixupInterface fixupInterface) {
		this.fixupInterface = fixupInterface;
	}
}