package io.openems.edge.io.iot2000.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class IOT2000Util {

	private static final String PIN_4 = "4";
	private static final String PIN_5 = "5";
	private static final String PIN_6 = "6";
	private static final String PIN_7 = "7";
	private static final String PIN_10 = "10";
	private static final String PIN_15 = "15";
	private static final String PIN_22 = "22";
	private static final String PIN_23 = "23";
	private static final String PIN_24 = "24";
	private static final String PIN_25 = "25";
	private static final String PIN_26 = "26";
	private static final String PIN_27 = "27";
	private static final String PIN_30 = "30";
	private static final String PIN_31 = "31";
	private static final String PIN_36 = "36";
	private static final String PIN_37 = "37";
	private static final String PIN_38 = "38";
	private static final String PIN_39 = "39";
	private static final String PIN_40 = "40";
	private static final String PIN_41 = "41";
	private static final String PIN_42 = "42";
	private static final String PIN_43 = "43";
	private static final String PIN_44 = "44";
	private static final String PIN_46 = "46";
	private static final String PIN_70 = "70";
	private static final String PIN_72 = "72";
	private static final String PIN_74 = "74";

	public static final String USER_LED = PIN_7;
	public static final String DQ_0 = PIN_40;
	public static final String DQ_1 = PIN_38;
	public static final String DI_0 = PIN_15;
	public static final String DI_1 = PIN_5;
	public static final String DI_2 = PIN_10;
	public static final String DI_3 = PIN_4;
	public static final String DI_4 = PIN_6;

	private static final String[] ALLPINS = { PIN_4, PIN_5, PIN_6, PIN_7, PIN_10, PIN_15, PIN_22, PIN_23, PIN_24,
			PIN_25, PIN_26, PIN_27, PIN_30, PIN_31, PIN_36, PIN_37, PIN_38, PIN_39, PIN_40, PIN_41, PIN_42, PIN_43,
			PIN_44, PIN_46, PIN_70, PIN_72, PIN_74 };

	private static final IOT2000Util OBJ = new IOT2000Util();

	private IOT2000Util() {
		this.setupPins();
	}

	public static IOT2000Util getInstance() {
		return OBJ;
	}

	private int setupPins() {

		int status = 0;

		for (String gpio : ALLPINS) {
			try {
				this.unexport(gpio);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				status -= 1;
			}

			try {
				this.export(gpio);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				status -= 1;
			}

		}

		if (status != 0) {
			return status;
		}

		// LED
		try {
			this.setDirectionLow(IOT2000Util.PIN_46);
			this.setDirectionLow(IOT2000Util.PIN_30);
			this.setDirectionIn(IOT2000Util.PIN_31);
			this.setIoValue(IOT2000Util.PIN_46, '0');
			this.setIoValue(IOT2000Util.PIN_30, '0');
			this.setDirectionLow(IOT2000Util.PIN_7);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			status++;
		}

		// DI0
		try {
			this.setDirectionIn(IOT2000Util.PIN_15);
			this.setDirectionHigh(IOT2000Util.PIN_42);
			this.setDirectionIn(IOT2000Util.PIN_43);
			this.setIoValue(IOT2000Util.PIN_42, '1');
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			status++;
		}

		// DI1
		try {
			this.setDirectionIn(IOT2000Util.PIN_5);
			this.setDirectionLow(IOT2000Util.PIN_44);
			this.setDirectionLow(IOT2000Util.PIN_72);
			this.setDirectionHigh(IOT2000Util.PIN_24);
			this.setDirectionIn(IOT2000Util.PIN_25);
			this.setIoValue(IOT2000Util.PIN_24, '1');
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			status++;
		}

		// DI2
		try {
			this.setDirectionIn(IOT2000Util.PIN_10);
			this.setDirectionLow(IOT2000Util.PIN_74);
			this.setDirectionHigh(IOT2000Util.PIN_26);
			this.setDirectionIn(IOT2000Util.PIN_25);
			this.setIoValue(IOT2000Util.PIN_26, '1');
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			status++;
		}

		// DI3
		try {
			this.setDirectionIn(IOT2000Util.PIN_4);
			this.setDirectionLow(IOT2000Util.PIN_70);
			this.setDirectionHigh(IOT2000Util.PIN_22);
			this.setDirectionIn(IOT2000Util.PIN_23);
			this.setIoValue(IOT2000Util.PIN_22, '1');
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			status++;
		}

		// DI4
		try {
			this.setDirectionIn(IOT2000Util.PIN_6);
			this.setDirectionHigh(IOT2000Util.PIN_36);
			this.setDirectionIn(IOT2000Util.PIN_37);
			this.setIoValue(IOT2000Util.PIN_36, '1');
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			status++;
		}

		// DQ0
		try {
			this.setDirectionLow(IOT2000Util.PIN_40);
			this.setDirectionIn(IOT2000Util.PIN_41);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			status++;
		}

		// DQ1
		try {
			this.setDirectionLow(IOT2000Util.PIN_38);
			this.setDirectionIn(IOT2000Util.PIN_39);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			status++;
		}

		return status;

	}

	private void unexport(String gpio) throws IOException {

		byte[] b = gpio.getBytes();
		File file = new File("/sys/class/gpio/unexport");
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(b);
		fos.close();
	}

	private void export(String gpio) throws IOException {
		byte[] b = gpio.getBytes();
		File file = new File("/sys/class/gpio/export");
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(b);
		fos.close();
	}

	private void setDirectionHigh(String gpio) throws IOException {
		this.setDirection(gpio, "high");
	}

	private void setDirectionLow(String gpio) throws IOException {
		this.setDirection(gpio, "low");
	}

	private void setDirectionIn(String gpio) throws IOException {
		this.setDirection(gpio, "in");
	}

	private void setDirection(String gpio, String dir) throws IOException {
		byte[] b = dir.getBytes();
		File file = new File("/sys/class/gpio/gpio" + gpio + "/direction");
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(b);
		fos.close();
	}

	private void setIoValue(String gpio, char val) throws IOException {
		File file = new File("/sys/class/gpio/gpio" + gpio + "/value");
		FileOutputStream fos = new FileOutputStream(file);
		fos.write((byte) val);
		fos.close();
	}

	public void setLedOn() {
		try {
			this.setIoValue(IOT2000Util.USER_LED, '1');
		} catch (IOException e) {
			// Ignore
		}
	}

	public void setLedOff() {
		try {
			this.setIoValue(IOT2000Util.USER_LED, '0');
		} catch (IOException e) {
			// Ignore
		}
	}

	public void setOutput(int out, boolean state) {

		String gpio;
		switch (out) {
		case 0:
			gpio = IOT2000Util.DQ_0;
			break;
		case 1:
			gpio = IOT2000Util.DQ_1;
			break;
		default:
			return;
		}

		char val = '0';
		if (state) {
			val = '1';
		}

		try {
			this.setIoValue(gpio, val);
		} catch (IOException e) {
			// Ignore
		}
	}

	public boolean getIoValue(String gpio) throws IOException {
		File file = new File("/sys/class/gpio/gpio" + gpio + "/value");
		FileInputStream fis = new FileInputStream(file);
		int value = fis.read();
		fis.close();

		if (value == 49) {
			return true;
		} else {
			return false;
		}

	}

}
