package io.openems.edge.io.hal.pi4j.modberry;

public class Cm4Hardware {
	
	private interface HardwareEnum {
		int getGpio();
	}
	
	public enum Led implements HardwareEnum {
		/**
		 * Red LED.
		 */
		LED_1(489),
		/**
		 * Green / Blue LED.
		 */
		LED_2(27);
		
		private int gpio;
		Led(int gpio) {
			this.gpio = gpio;
		}
		
		/**
		 * Get GPIO of the LED.
		 */
		public int getGpio() {
			return this.gpio;
		}
	}
	
	public enum Button implements HardwareEnum {
		/**
		 * User button 1.
		 */
		BUTTON_1(13);
		
		private int gpio;
		Button(int gpio) {
			this.gpio = gpio;
		}
		
		public int getGpio() {
			return this.gpio;
		}
	}
}
