package io.openems.edge.ess.mr.gridcon.onoffgrid.state;

import java.time.LocalDateTime;
import java.util.BitSet;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.soltaro.SoltaroBattery;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.GridconPCS;
import io.openems.edge.ess.mr.gridcon.StateObject;
import io.openems.edge.ess.mr.gridcon.WeightingHelper;

public abstract class BaseState implements StateObject {

	public static final float ONLY_ON_GRID_FREQUENCY_FACTOR = 1.0f;
	public static final float ONLY_ON_GRID_VOLTAGE_FACTOR = 1.0f;
	
	private ComponentManager manager;
	protected String gridconPCSId;
	protected String battery1Id;
	protected String battery2Id;
	protected String battery3Id;
	
	public BaseState(ComponentManager manager, String gridconPCSId, String b1Id, String b2Id, String b3Id) {
		this.manager = manager;
		this.gridconPCSId = gridconPCSId;
		this.battery1Id = b1Id;
		this.battery2Id = b2Id;
		this.battery3Id = b3Id;		
	}
	
	protected boolean isNextStateUndefined() {
		return !isGridconDefined() || !isAtLeastOneBatteryDefined();
	}


	private boolean isAtLeastOneBatteryDefined() {
		boolean undefined = true;
		
		if (getBattery1() != null) {
			undefined = undefined && getBattery1().isUndefined();
		}
		if (getBattery2() != null) {
			undefined = undefined && getBattery2().isUndefined();
		}
		if (getBattery3() != null) {
			undefined = undefined && getBattery3().isUndefined();
		}
		
		return !undefined;
	}

	private boolean isGridconDefined() {
		// TODO when is it defined
		return true;
	}

	protected boolean isNextStateError() {
		if (getGridconPCS() != null && getGridconPCS().isError()) {
			return true;
		}
		
		if (getBattery1() != null && getBattery1().isError()) {
			return true;
		}
		
		if (getBattery2() != null && getBattery2().isError()) {
			return true;
		}
		
		if (getBattery3() != null && getBattery3().isError()) {
			return true;
		}
		
		return false;
	}
	
	protected boolean isNextStateStopped() {
		return getGridconPCS() != null && getGridconPCS().isStopped();
	}
	
	protected boolean isNextStateRunning() {
		return (getGridconPCS() != null && getGridconPCS().isRunning());
	}

	protected void startBatteries() {
		if (getBattery1() != null) {
		if (!getBattery1().isRunning()) {
			getBattery1().start();
		}
		}
		if (getBattery2() != null) {
		if (!getBattery2().isRunning()) {
			getBattery2().start();
		}
		}
		if (getBattery3() != null) {
		if (!getBattery3().isRunning()) {
			getBattery3().start();
		}
		}
	}

	protected boolean isBatteriesStarted() {
		boolean running = true;
		if (getBattery1() != null) {
			running = running && getBattery1().isRunning();
		}
		if (getBattery2() != null) {
			running = running && getBattery2().isRunning();
		}
		if (getBattery3() != null) {
			running = running && getBattery3().isRunning();
		}
		return running;
	}
	
	protected void setStringControlMode() {
		int weightingMode = WeightingHelper.getStringControlMode(getBattery1(), getBattery2(), getBattery3());
		getGridconPCS().setStringControlMode(weightingMode);
	}
		
	protected void setStringWeighting() {
		float activePower = getGridconPCS().getActivePower();
		
		Float[] weightings = WeightingHelper.getWeighting(activePower, getBattery1(), getBattery2(), getBattery3());
		
		getGridconPCS().setWeightStringA(weightings[0]);
		getGridconPCS().setWeightStringB(weightings[1]);
		getGridconPCS().setWeightStringC(weightings[2]);
		
	}
	
	protected void setDateAndTime() {
		int date = this.convertToInteger(this.generateDate(LocalDateTime.now()));
		getGridconPCS().setSyncDate(date);
		int time = this.convertToInteger(this.generateTime(LocalDateTime.now()));
		getGridconPCS().setSyncTime(time);
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
	
	GridconPCS getGridconPCS() {
		return getComponent(gridconPCSId);
	}
	
	SoltaroBattery getBattery1() {
		return getComponent(battery1Id);
	}
	
	SoltaroBattery getBattery2() {
		return getComponent(battery2Id);
	}
	
	SoltaroBattery getBattery3() {
		return getComponent(battery3Id);
	}
	
	<T> T getComponent(String id) {
		T component = null;
		try {
			component = manager.getComponent(id);
		} catch (OpenemsNamedException e) {
			
		}
		return component;
	}
}
