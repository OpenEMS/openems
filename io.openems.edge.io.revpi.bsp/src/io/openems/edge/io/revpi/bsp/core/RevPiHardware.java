package io.openems.edge.io.revpi.bsp.core;

import java.io.IOException;

import org.clehne.revpi.dataio.DataInOut;

/**
 * Provides access to the kunbus revpi IO library.
 */
public class RevPiHardware {

    public interface RevPiDigitalIo {

	/**
	 * Gets the digital value of the corresponding digital channel.
	 * 
	 * @param revPiIoName the channel alias
	 * @return the digital value
	 * @throws IOException on error
	 */
	public boolean getDigital(String revPiIoName) throws IOException;

	public void setDigital(String revPiIoName, Boolean value) throws IOException;

	/**
	 * Closes the conntection.
	 * 
	 * @throws IOException on error
	 */
	public void close() throws IOException;
    }

    private static RevPiHardware instance;
    private final DataInOut revPiHardwareAccess;
    private final RevPiBoard revPiBoard;
    private final RevPiDigitalIo digitalIO;

    private RevPiHardware() {
	this.revPiHardwareAccess = new DataInOut();
	this.revPiBoard = new RevPiBoard(this.revPiHardwareAccess);
	this.digitalIO = new RevPiDigitalIo() {

	    @Override
	    public void setDigital(String revPiIoName, Boolean value) throws IOException {
		RevPiHardware.this.revPiHardwareAccess.setDigital(revPiIoName, value);
	    }

	    @Override
	    public boolean getDigital(String revPiIoName) throws IOException {
		return RevPiHardware.this.revPiHardwareAccess.getDigital(revPiIoName);
	    }

	    @Override
	    public void close() throws IOException {
		; // we silently ignore a close request
	    }
	};
    }

    /**
     * Gets the instance of the {@link RevPiHardware}.
     * 
     * @return the RevPiHardware
     */
    public static synchronized RevPiHardware get() {
	if (instance == null) {
	    instance = new RevPiHardware();
	}
	return instance;
    }

    public RevPiBoard getBoard() {
	return this.revPiBoard;
    }

    public RevPiDigitalIo getDigitalIo() {
	return this.digitalIO;
    }

}
