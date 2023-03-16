package io.openems.edge.io.hal.modberry;

public class Cm4Hardware {
		
	private interface HardwareEnum {
		int getGpio();
	}
	
	/**
	 * Represents the hardware LED. 
	 * The red LED on pin 489 only works if you load the MCP23008 driver
	 * on address 0x20. To do this, you need to modify your hardware overlay.
	 */
	public enum Led implements HardwareEnum {
		/**
		 * Green LED on the front of the Modberry X500 CM4.
		 */
		LED_1(27),
		LED_2(489);
		
		private int gpio;

		Led(int gpio) {
			this.gpio = gpio;
		}
		
		/**
		 * Get GPIO of the LED.
		 * @return number of the GPIO which the LED is occupying.
		 */
		public int getGpio() {
			return this.gpio;
		}
	}
	
	/**
	 * Represents the press-button hardware.
	 *
	 */
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
	
	/**
	 * Represents the digital outputs on the hardware.
	 * These pins are output only.
	 *
	 */
	public enum DigitalOut implements HardwareEnum {
		
		DOUT_1(22),
		DOUT_2(23),
		DOUT_3(24),
		DOUT_4(25);
		
		private int gpio;

		DigitalOut(int gpio) {
			this.gpio = gpio;
		}
		
		public int getGpio() {
			return this.gpio;
		}
	}
	
	/**
	 * Represents the galvanically isolated inputs of the hardware.
	 * These inputs are input only.
	 */
	public enum OptoDigitalIn implements HardwareEnum {
		
		DIN_1(18),
		DIN_2(19),
		DIN_3(20),
		DIN_4(21);
		
		private int gpio;

		OptoDigitalIn(int gpio) {
			this.gpio = gpio;
		}
		
		public int getGpio() {
			return this.gpio;
		}
	}
	
	/**
	 * Represents the programmable digital in/outputs of the hardware.
	 * At one time, only one direction can be used.
	 * To change the direction at runtime, close the previous one and create a new GPIO object.
	 *
	 */
	public enum BidirectionalIo implements HardwareEnum {
		
		DI_1(500),
		DO_1(496),
		
		DI_2(501),
		DO_2(497),
		
		DI_3(502),
		DO_3(498),
		
		DI_4(503),
		DO_4(499);
		
		
		private int gpio;

		BidirectionalIo(int gpio) {
			this.gpio = gpio;
		}
		
		public int getGpio() {
			return this.gpio;
		}
	}
	
	/**
	 * The built-in hardware buzzer.
	 */
	public enum Buzzer implements HardwareEnum {
		BUZZER_0(493);
		
		private int gpio;

		Buzzer(int gpio) {
			this.gpio = gpio;
		}
		
		public int getGpio() {
			return this.gpio;
		}
	}
}
