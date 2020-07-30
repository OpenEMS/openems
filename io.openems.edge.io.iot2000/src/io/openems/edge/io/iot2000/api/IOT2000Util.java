package io.openems.edge.io.iot2000.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class IOT2000Util {

	private static final String[] ALLPINS = { "7", "46", "30", "31", "15", "42", "43", "5", "44", "72", "24", "25",
			"10", "74", "26", "27", "4", "70", "22", "23", "6", "36", "37", "40", "41", "38", "39" };

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
			this.setDirLow("46");
			this.setDirLow("30");
			this.setDirIn("31");
			this.setVal("46", '0');
			this.setVal("30", '0');
			this.setDirLow("7");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			status++;
		}

		// DI0
		try {
			this.setDirIn("15");
			this.setDirHigh("42");
			this.setDirIn("43");
			this.setVal("42", '1');
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			status++;
		}

		// DI1
		try {
			this.setDirIn("5");
			this.setDirLow("44");
			this.setDirLow("72");
			this.setDirHigh("24");
			this.setDirIn("25");
			this.setVal("24", '1');
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			status++;
		}

		// DI2
		try {
			this.setDirIn("10");
			this.setDirLow("74");
			this.setDirHigh("26");
			this.setDirIn("25");
			this.setVal("26", '1');
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			status++;
		}

		// DI3
		try {
			this.setDirIn("4");
			this.setDirLow("70");
			this.setDirHigh("22");
			this.setDirIn("23");
			this.setVal("22", '1');
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			status++;
		}

		// DI4
		try {
			this.setDirIn("6");
			this.setDirHigh("36");
			this.setDirIn("37");
			this.setVal("36", '1');
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			status++;
		}

		// DQ0
		try {
			this.setDirLow("40");
			this.setDirIn("41");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			status++;
		}

		// DQ1
		try {
			this.setDirLow("38");
			this.setDirIn("39");
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

	private void setDirHigh(String gpio) throws IOException {
		this.setDir(gpio, "high");
	}

	private void setDirLow(String gpio) throws IOException {
		this.setDir(gpio, "low");
	}

	private void setDirIn(String gpio) throws IOException {
		this.setDir(gpio, "in");
	}

	private void setDir(String gpio, String dir) throws IOException {
		byte[] b = dir.getBytes();
		File file = new File("/sys/class/gpio/gpio" + gpio + "/direction");
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(b);
		fos.close();
	}

	private void setVal(String gpio, char val) throws IOException {
		File file = new File("/sys/class/gpio/gpio" + gpio + "/value");
		FileOutputStream fos = new FileOutputStream(file);
		fos.write((byte) val);
		fos.close();
	}

	public void setLedOn() {
		try {
			this.setVal("7", '1');
		} catch (IOException e) {
			// Ignore
		}
	}

	public void setLedOff() {
		try {
			this.setVal("7", '0');
		} catch (IOException e) {
			// Ignore
		}
	}

	public void setOutput(int out, boolean state) {

		String gpio;
		switch (out) {
		case 0:
			gpio = "40";
			break;
		case 1:
			gpio = "38";
			break;
		default:
			return;
		}

		char val = '0';
		if (state) {
			val = '1';
		}

		try {
			this.setVal(gpio, val);
		} catch (IOException e) {
			// Ignore
		}
	}

	public boolean getIoValue(int gpio) throws IOException {
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
