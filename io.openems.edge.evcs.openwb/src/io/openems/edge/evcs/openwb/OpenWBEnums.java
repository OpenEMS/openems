package io.openems.edge.evcs.openwb;

public class OpenWBEnums {
	   enum ChargePoint{
	       CP0(0), CP1(1);
	       private int value;
	       private ChargePoint(int value) {
	            this.value = value;
	       }
	       public int getValue(){
	        return value;
	       }
	   }

}