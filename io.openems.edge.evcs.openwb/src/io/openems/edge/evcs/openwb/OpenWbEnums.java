package io.openems.edge.evcs.openwb;

public class OpenWbEnums {
	   enum ChargePoint {
	       CP0(0), 
	       CP1(1);
	       
		   private int value;
	       
	       private ChargePoint(int value) {
	            this.value = value;
	       }
	       
	       public int getValue() {
	        return this.value;
	       }
	   }

}