package io.openems.edge.bridge.http;

final class OsDetector {
	
	public enum Plattform {
		Windows,
		Mac,
		Linux
	}
	
	static Plattform findOs() {
		var os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win")) {
			return Plattform.Windows;
		} else if (os.contains("mac")) {
			return Plattform.Mac;
		} else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
			return Plattform.Linux;
		} else {
			throw new IllegalPlattformException("Plattform " + os + "not supported.");
		}
			
	}
}
