package io.openems.edge.io.opendtu.inverter;

import java.util.ArrayList;
import java.util.List;

public class InverterData {
	private String serialNumber;
	private String phase;

	private int power;
	private int maxPower;
	private int voltage;
	private int current;
	private int frequency;

	private int limitType; // 0 relative; 1 absolute
	private int limitAbsolute;
	private int limitAbsoluteWanted;
	private int limitRelative;
	private int limitHardware;
	private String limitStatus;
	private static int totalLimitAbssolute = 0;
	private static int totalPower = 0;
	private static int totalLimitHardware = 0;
	private long lastUpdate;
	
	

	public long getLastUpdate() {
		return this.lastUpdate;
	}

	public void setLastUpdate(long lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public InverterData(String serialNumber, String phase) {
		this.serialNumber = serialNumber;
		this.phase = phase;
	}

	public String getSerialNumber() {
		return this.serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public String getPhase() {
		return this.phase;
	}

	public void setPhase(String phase) {
		this.phase = phase;
	}

	public int getPower() {
		return this.power;
	}

    public void setPower(int power) {
        synchronized (InverterData.class) {
            totalPower -= this.power;
            totalPower += power;
        }
        this.power = power;
    }

    public static int getTotalPower() {
    	return totalPower;
    }

	public int getMaxPower() {
		return this.maxPower;
	}

	public int getCurrent() {
		return this.current;
	}

	public void setCurrent(int current) {
		this.current = current;
	}

	public int getVoltage() {
		return this.voltage;
	}

	public void setVoltage(int voltage) {
		this.voltage = voltage;
	}

	public int getFrequency() {
		return this.frequency;
	}

	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	public int getLimitType() {
		return this.limitType;
	}

	public void setLimitType(int limitType) {
		this.limitType = limitType;
	}

	/**
	 * Retrieves the current status of the limit setting.
	 * 
	 * <p>
	 * This method returns a {@code String} that represents the current status of
	 * the limit setting for an inverter. The status indicates whether the limit has
	 * been successfully set, is pending, or has encountered an error.
	 * 
	 * @return A {@code String} representing the current status of the limit
	 *         setting.
	 */
	public String getLimitStatus() {
		return this.limitStatus;
	}

	public void setLimitStatus(String limitStatus) {
		this.limitStatus = limitStatus;
	}
	
	/**
	 * Retrieves the hardware limit of the inverter.
	 *
	 * @return The hardware limit of the inverter, expressed as an integer.
	 */
	public int getLimitHardware() {
	    return this.limitHardware;
	}


    public void setLimitHardware(int limitHardware) {
        synchronized (InverterData.class) {
            totalLimitHardware -= this.limitHardware;
            totalLimitHardware += limitHardware;
        }
        this.limitHardware = limitHardware;
	}	
    
    public static int getTotalLimitHardware() {
    	return totalLimitHardware;
    }
	
	
	public int getLimitRelative() {
		return this.limitRelative;
	}

	public void setLimitRelative(int limitRelative) {
		this.limitRelative = limitRelative;
	}
	
	public int getLimitAbsoluteWanted() {
		return this.limitAbsoluteWanted;
	}
	
	public void setLimitAbsoluteWanted(int limitAbsolteWanted) {
		this.limitAbsoluteWanted = limitAbsolteWanted;
	}

	public int getLimitAbsolute() {
		return this.limitAbsolute;
	}
	
    public void setLimitAbsolute(int limitAbsolute) {
        // Adjust the total sum when updating the PowerLimitAbsolute value
        // Subtract the old value and add the new value to the total
        synchronized (InverterData.class) {
            totalLimitAbssolute -= this.limitAbsolute;
            totalLimitAbssolute += limitAbsolute;
        }
        this.limitAbsolute = limitAbsolute;
        //this.lastUpdate = System.currentTimeMillis(); // Update the lastUpdate timestamp

	}

	/**
	 * Static method to get the total sum of CurrentPowerLimitAbsolute across all
	 * instances.
	 * 
	 * @return The total sum of CurrentPowerLimitAbsolute.
	 */
	public static int getTotalLimitAbsolute() {
		return totalLimitAbssolute;
	}

	static List<InverterData> collectInverterData(Config config) {
		List<InverterData> validInverters = new ArrayList<>();

		if (!config.serialNumberL1().isEmpty()) {
			validInverters.add(new InverterData(config.serialNumberL1(), "L1"));
		}
		if (!config.serialNumberL2().isEmpty()) {
			validInverters.add(new InverterData(config.serialNumberL2(), "L2"));
		}
		if (!config.serialNumberL3().isEmpty()) {
			validInverters.add(new InverterData(config.serialNumberL3(), "L3"));
		}

		return validInverters;
	}

}