package io.openems.edge.deye.enums;
import org.apache.log4j.Level;

import io.openems.common.types.OptionsEnum;

public enum ErrorCodes implements OptionsEnum {
	UNDEFINED(-1, "Undefined", Level.INFO), //
	F0(0, "No Error", Level.ERROR), //
	F7(7, "DC soft start error", Level.ERROR),
	F10(10, "AUX power board error", Level.ERROR),
    F13(13, "Working mode change", Level.ERROR),                              
    F18(18, "Hardware AC overcurrent", Level.ERROR),                          
    F20(20, "Hardware DC overcurrent", Level.ERROR),                          
    F22(22, "Emergency stop fault", Level.ERROR),                             
    F23(23, "Instantaneous leakage current fault", Level.ERROR),              
    F24(24, "Phalanx insulation resistance fault", Level.ERROR),              
    F26(26, "Parallel CAN-Bus communication failure", Level.ERROR),           
    F29(29, "No AC grid", Level.ERROR),                                       
    F35(35, "Parallel system shutdown failure", Level.ERROR),                 
    F41(41, "Parallel system stop", Level.ERROR),                             
    F42(42, "AC line low voltage fault", Level.ERROR),                        
    F46(46, "Backup battery failure", Level.ERROR),                           
    F49(49, "Backup battery failure", Level.ERROR),                           
    F47(47, "AC overfrequency", Level.ERROR),                                 
    F48(48, "AC underfrequency", Level.ERROR),                                
    F56(56, "Bus voltage too low", Level.ERROR),                              
    F58(58, "BMS communication failure", Level.ERROR),                        
    F63(63, "Arc fault", Level.ERROR),                                        
    F64(64, "Heat sink temperature too high", Level.ERROR)
	; 
	
    private final String name;
    private final int value;
    private final Level level;

    private ErrorCodes(int value, String name, Level level) {
        this.value = value;
        this.name = name;
		this.level = level;
    }

	@Override
	public int getValue() {
		return this.value;
	}
	
	public Level getLevel() {
		return this.level;
	}	

	@Override
	public String getName() {
		return this.name;
	}

	public static ErrorCodes fromInt(int code) {
	    for (ErrorCodes e : values()) {
	        if (e.getValue() == code) {
	            return e;
	        }
	    }	
	    return UNDEFINED;
	}
	
	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}	
		
	

}