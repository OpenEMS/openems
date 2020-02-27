package io.openems.edge.ess.mr.gridcon.ongrid.state;

import java.time.LocalDateTime;
import java.util.BitSet;

import io.openems.edge.ess.mr.gridcon.EssGridcon;
import io.openems.edge.ess.mr.gridcon.State;
import io.openems.edge.ess.mr.gridcon.battery.SoltaroBattery;

public abstract class BaseState implements State {

	protected EssGridcon gridconPCS;
	protected SoltaroBattery battery1;
	protected SoltaroBattery battery2;
	protected SoltaroBattery battery3;
	
	public BaseState(EssGridcon gridconPCS, SoltaroBattery b1, SoltaroBattery b2, SoltaroBattery b3) {
		this.gridconPCS = gridconPCS;
		this.battery1 = b1;
		this.battery2 = b2;
		this.battery3 = b3;		
	}
	
	protected boolean isNextStateUndefined() {
		return false;
	}

	protected boolean isNextStateError() {
		if (gridconPCS != null && gridconPCS.isError()) {
			return true;
		}
		
		if (battery1 != null && battery1.isError()) {
			return true;
		}
		
		if (battery2 != null && battery2.isError()) {
			return true;
		}
		
		if (battery3 != null && battery3.isError()) {
			return true;
		}
		
		return false;
	}
	
	protected boolean isNextStateStopped() {
		return gridconPCS != null && gridconPCS.isStopped();
	}
	
	protected boolean isNextStateRunning() {
		boolean running = gridconPCS != null && battery1 != null && battery2 != null && battery3 != null;
		if (gridconPCS != null) {
			running = running && gridconPCS.isRunning();
		}
		
		if (battery1 != null) {
			running = running && battery1.isRunning();
		}
		
		if (battery2 != null) {
			running = running && battery2.isRunning();
		}
		
		if (battery3 != null) {
			running = running && battery3.isRunning();
		}
		return running;
	}

	protected void startBatteries() {
		if (battery1 != null) {
		if (!battery1.isRunning()) {
			battery1.start();
		}
		}
		if (battery2 != null) {
		if (!battery2.isRunning()) {
			battery2.start();
		}
		}
		if (battery3 != null) {
		if (!battery3.isRunning()) {
			battery3.start();
		}
		}
	}

	protected boolean isBatteriesStarted() {
		boolean running = true;
		if (battery1 != null) {
			running = running && battery1.isRunning();
		}
		if (battery2 != null) {
			running = running && battery2.isRunning();
		}
		if (battery3 != null) {
			running = running && battery3.isRunning();
		}
		return running;
	}
	
	protected void setStringControlMode() {
		int weightingMode = WeightingHelper.getStringControlMode(battery1, battery2, battery3);
		gridconPCS.setStringControlMode(weightingMode);
	}
		
	protected void setStringWeighting() {
		int activePower = gridconPCS.getActivePower().value().orElse(0);
		
		Float[] weightings = WeightingHelper.getWeighting(activePower, battery1, battery2, battery3);
		
		gridconPCS.setWeightStringA(weightings[0]);
		gridconPCS.setWeightStringB(weightings[1]);
		gridconPCS.setWeightStringC(weightings[2]);
		
	}
	
	protected void setDateAndTime() {
		int date = this.convertToInteger(this.generateDate(LocalDateTime.now()));
		gridconPCS.setSyncDate(date);
		int time = this.convertToInteger(this.generateTime(LocalDateTime.now()));
		gridconPCS.setSyncTime(time);
	}
	
	private BitSet generateDate(LocalDateTime time) {
		byte dayOfWeek = (byte) time.getDayOfWeek().ordinal();
		byte day = (byte) time.getDayOfMonth();
		byte month = (byte) time.getMonth().getValue();
		byte year = (byte) (time.getYear() - 2000); // 0 == year 2000 in the protocol

		return BitSet.valueOf(new byte[] { day, dayOfWeek, year, month });
	}

	private BitSet generateTime(LocalDateTime time) {
		byte seconds = (byte) time.getSecond();
		byte minutes = (byte) time.getMinute();
		byte hours = (byte) time.getHour();
		// second byte is unused
		return BitSet.valueOf(new byte[] { seconds, 0, hours, minutes });
	}

	private int convertToInteger(BitSet bitSet) {
		long[] l = bitSet.toLongArray();
		if (l.length == 0) {
			return 0;
		}
		return (int) l[0];
	}
}
