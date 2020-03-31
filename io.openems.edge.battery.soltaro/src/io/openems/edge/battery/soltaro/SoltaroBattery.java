package io.openems.edge.battery.soltaro;

import io.openems.edge.battery.api.Battery;

public interface SoltaroBattery extends Battery {
	
	void start();
	void stop();
	
	boolean isRunning();
	boolean isStopped();
	boolean isError();
	
	boolean isUndefined();

}
