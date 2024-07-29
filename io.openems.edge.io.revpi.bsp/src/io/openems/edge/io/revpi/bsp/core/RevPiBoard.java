package io.openems.edge.io.revpi.bsp.core;

import java.io.IOException;

import org.clehne.revpi.dataio.DataInOut;

/**
 * LED Bitpattern. <br/>
 * Bit 0 1 2 3 4 5 6 7 <br/>
 * A1 0 0 x x aus..... <br/>
 * A1 1 0 x x gruen... <br/>
 * A1 0 1 x x rot..... <br/>
 * A1 1 1 x x orange...<br/>
 * <br/>
 * A2 x x 0 0 aus..... <br/>
 * A2 x x 1 0 gruen... <br/>
 * A2 x x 0 1 rot..... <br/>
 * A2 x x 1 1 orange.. <br/>
 * <br/>
 * A3 0 0 aus.......<br/>
 * A3 1 0 gruen.....<br/>
 * A3 0 1 rot.......<br/>
 * A3 1 1 orange....<br/>
 * 
 * <p>
 * Relais 0 öffne Relais 1 schliesse Relais (Relais ist per default geschlossen)
 * WDG 0/1 toggle every 60s
 */
public class RevPiBoard {

    private final DataInOut revPiHardwareAccess;
    private static final String REVPI_LED = "RevPiLED";

    public RevPiBoard(DataInOut revPiHardwareAccess) {
	this.revPiHardwareAccess = revPiHardwareAccess;
    }

    /**
     * Toggles the watchdog.
     * 
     * @throws IOException on error
     */
    public void toggleWatchdog() throws IOException {

	int value = this.revPiHardwareAccess.getValue(REVPI_LED);
	// toggle watchdog
	if ((value & DataInOut.PICONTROL_WD_TRIGGER) == 0) {
	    value |= DataInOut.PICONTROL_WD_TRIGGER;
	} else {
	    value &= ~DataInOut.PICONTROL_WD_TRIGGER;
	}
	this.revPiHardwareAccess.setValue(REVPI_LED, (value & 0xff));
    }

    /**
     * Gets the state of the internal relais.
     * 
     * @return the state
     * @throws IOException on error
     */
    public Boolean getRelaisState() throws IOException {
	int value = this.revPiHardwareAccess.getValue(REVPI_LED);
	if ((value & DataInOut.PICONTROL_X2_DOUT) != 0) {
	    return true;
	}
	return false;
    }

    public void setRelaisState(boolean setRelais) throws IOException {
	int value = this.revPiHardwareAccess.getValue(REVPI_LED);
	if (setRelais) {
	    this.revPiHardwareAccess.setValue(REVPI_LED, (value | DataInOut.PICONTROL_X2_DOUT));
	} else {
	    this.revPiHardwareAccess.setValue(REVPI_LED, ((value & ~DataInOut.PICONTROL_X2_DOUT) & 0xff));
	}
    }

    public void setA1Red() throws IOException {
	int value = this.revPiHardwareAccess.getValue(REVPI_LED);
	value &= ~DataInOut.PICONTROL_LED_A1_MASK;
	this.revPiHardwareAccess.setValue(REVPI_LED, (value | DataInOut.PICONTROL_LED_A1_RED) & 0xff);
    }

    public void setA1Green() throws IOException {
	int value = this.revPiHardwareAccess.getValue(REVPI_LED);
	value &= ~DataInOut.PICONTROL_LED_A1_MASK;
	this.revPiHardwareAccess.setValue(REVPI_LED, (value | DataInOut.PICONTROL_LED_A1_GREEN) & 0xff);
    }

    public void setA1Off() throws IOException {
	int value = this.revPiHardwareAccess.getValue(REVPI_LED);
	value &= ~DataInOut.PICONTROL_LED_A1_MASK;
	this.revPiHardwareAccess.setValue(REVPI_LED, (value) & 0xff);
    }

    public void setA2Red() throws IOException {
	int value = this.revPiHardwareAccess.getValue(REVPI_LED);
	value &= ~DataInOut.PICONTROL_LED_A2_MASK;
	this.revPiHardwareAccess.setValue(REVPI_LED, (value | DataInOut.PICONTROL_LED_A2_RED) & 0xff);
    }

    public void setA2Green() throws IOException {
	int value = this.revPiHardwareAccess.getValue(REVPI_LED);
	value &= ~DataInOut.PICONTROL_LED_A2_MASK;
	this.revPiHardwareAccess.setValue(REVPI_LED, (value | DataInOut.PICONTROL_LED_A2_GREEN) & 0xff);
    }

    public void setA2Off() throws IOException {
	int value = this.revPiHardwareAccess.getValue(REVPI_LED);
	value &= ~DataInOut.PICONTROL_LED_A2_MASK;
	this.revPiHardwareAccess.setValue(REVPI_LED, (value) & 0xff);
    }

    public void setA3Red() throws IOException {
	int value = this.revPiHardwareAccess.getValue(REVPI_LED);
	value &= ~DataInOut.PICONTROL_LED_A3_MASK;
	this.revPiHardwareAccess.setValue(REVPI_LED, (value | DataInOut.PICONTROL_LED_A3_RED) & 0xff);
    }

    public void setA3Green() throws IOException {
	int value = this.revPiHardwareAccess.getValue(REVPI_LED);
	value &= ~DataInOut.PICONTROL_LED_A3_MASK;
	this.revPiHardwareAccess.setValue(REVPI_LED, (value | DataInOut.PICONTROL_LED_A3_GREEN) & 0xff);
    }

    public void setA3Off() throws IOException {
	int value = this.revPiHardwareAccess.getValue(REVPI_LED);
	value &= ~DataInOut.PICONTROL_LED_A3_MASK;
	this.revPiHardwareAccess.setValue(REVPI_LED, (value) & 0xff);
    }

    /**
     * Closes the connection.
     */
    public void close() {
    }

}
