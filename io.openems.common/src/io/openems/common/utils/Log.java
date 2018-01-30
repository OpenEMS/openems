package io.openems.common.utils;

public class Log {
	public static final void info(String message) {
		System.out.println("INFO " + message);
	}
	
	public static final void warn(String message) {
		System.out.println("WARN " + message);
	}
	
	public static final void error(String message) {
		System.out.println("ERROR " + message);
	}
	
	public static final void error(String message, Exception e) {
		System.out.println("ERROR " + message);
		e.printStackTrace();
	}
}
