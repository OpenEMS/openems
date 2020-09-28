package io.openems.edge.ess.mr.gridcon.state.onoffgrid;

import java.time.LocalDateTime;
import java.util.BitSet;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.GridconPcs;
import io.openems.edge.ess.mr.gridcon.Helper;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.StateObject;
import io.openems.edge.ess.mr.gridcon.WeightingHelper;
import io.openems.edge.meter.api.SymmetricMeter;

public abstract class BaseState implements StateObject {

	private static final int VOLTAGE_GRID = 200_000;
	public static final float ONOFF_GRID_VOLTAGE_FACTOR = 1;
	public static final float ONOFF_GRID_FREQUENCY_FACTOR_ONLY_ONGRID = 1.054f;

	private ComponentManager manager;
	private String gridconPcsId;
	private String battery1Id;
	private String battery2Id;
	private String battery3Id;
	private String inputNA1;
	private boolean na1Inverted;
	private String inputNA2;
	private boolean na2Inverted;
	private String inputSyncBridge;
	private String outputSyncBridge;
	private String meterId;
	private IState stateBefore;
	protected DecisionTableCondition condition; // TODO besser wäre es die Methode IState getNextState(); so zu deklarieren
	private StateObject subStateObject;
	// IState getNextState(DecisionTableCondition condition), das erfordert aber ein
	// anderes Handling der states....
	// was nur konsequent ist, da on grid states und off grid states aktuell nicht
	// konsistent sind...
	// Die aktuelle state machine für den ongrid state ist eigtl eine "unter state
	// machine der gesamt state machine und bildet nur den on grid betrieb ab

	public BaseState(ComponentManager manager, DecisionTableCondition condition, String gridconPcsId, String b1Id,
			String b2Id, String b3Id, String inputNA1, String inputNA2, String inputSyncBridge, String outputSyncBridge,
			String meterId, boolean na1Inverted, boolean na2Inverted) {
		this.manager = manager;
		this.gridconPcsId = gridconPcsId;
		this.battery1Id = b1Id;
		this.battery2Id = b2Id;
		this.battery3Id = b3Id;
		this.inputNA1 = inputNA1;
		this.inputNA2 = inputNA2;
		this.inputSyncBridge = inputSyncBridge;
		this.outputSyncBridge = outputSyncBridge;
		this.meterId = meterId;
		this.condition = condition;
		this.na1Inverted = na1Inverted;
		this.na2Inverted = na2Inverted;
	}

	protected boolean isNextStateUndefined() {
		return !isGridconDefined() || !isAtLeastOneBatteryDefined() || !isDigitalInputsDefined();
	}

	private boolean isDigitalInputsDefined() {
		boolean defined = true;

		defined = defined && isDigitalInputDefined(inputNA1);
		defined = defined && isDigitalInputDefined(inputNA2);
		defined = defined && isDigitalInputDefined(inputSyncBridge);

		return defined;
	}

	private boolean isDigitalInputDefined(String inputAddress) {
		boolean defined = false;
		try {
			BooleanReadChannel inputChannel = getBooleanReadChannel(inputAddress);
			defined = isValueDefined(inputChannel.value());
		} catch (Exception e) {
			defined = false;
		}
		return defined;
	}

	private boolean isValueDefined(Value<Boolean> value) {
		if (value != null) {
			return value.isDefined();
		}
		return false;
	}

	private BooleanReadChannel getBooleanReadChannel(String channelAddress)
			throws IllegalArgumentException, OpenemsNamedException {
		return this.manager.getChannel(ChannelAddress.fromString(channelAddress));
	}

	private boolean isAtLeastOneBatteryDefined() {
		boolean undefined = true;

		if (getBattery1() != null) {
			undefined = undefined && Helper.isUndefined(getBattery1());
		}
		if (getBattery2() != null) {
			undefined = undefined && Helper.isUndefined(getBattery2());
		}
		if (getBattery3() != null) {
			undefined = undefined && Helper.isUndefined(getBattery3());
		}

		return !undefined;
	}

	private boolean isGridconDefined() {
		// TODO when is it defined
		return true;
	}

	protected boolean isNextStateError() {
		if (getGridconPcs() != null && getGridconPcs().isError()) {
			return true;
		}

		if (getBattery1() != null && Helper.isError(getBattery1())) {
			return true;
		}

		if (getBattery2() != null && Helper.isError(getBattery2())) {
			return true;
		}

		if (getBattery3() != null && Helper.isError(getBattery3())) {
			return true;
		}

		return false;
	}

	protected boolean isNextStateOnGridStopped() {
		return isSystemOngrid() && getGridconPcs() != null && getGridconPcs().isStopped();
	}

	protected boolean isNextStateOnGridRunning() {
		return isSystemOngrid() && (getGridconPcs() != null && getGridconPcs().isRunning());
	}

	protected boolean isNextStateOffGrid() {
		if (isSystemOffgrid() && !isVoltageOnMeter()) {
			return true;
		}
		return false;
	}

	protected boolean isNextStateGoingOnGrid() {
		if (isSystemOffgrid() && isVoltageOnMeter()) {
			return true;
		}
		return false;
	}

	private boolean isVoltageOnMeter() {
		boolean ret = false;
		try {
			SymmetricMeter meter = manager.getComponent(meterId);
			Value<Integer> voltageValue = meter.getVoltage();
			int voltage = voltageValue.orElse(0); // voltage is in mV
			ret = voltage > BaseState.VOLTAGE_GRID;
		} catch (OpenemsNamedException e) {
			ret = false;
		}
		return ret;
	}

	protected float getVoltageOnMeter() {
		float ret = Float.MIN_VALUE;
		try {
			SymmetricMeter meter = manager.getComponent(meterId);
			Value<Integer> voltageValue = meter.getVoltage();
			int voltage = voltageValue.orElse(0); // voltage is in mV
			ret = voltage / 1000.0f;
		} catch (OpenemsNamedException e) {
			System.out.println(e);
		}
		return ret;
	}

	protected float getFrequencyOnMeter() {
		float ret = Float.MIN_VALUE;
		try {
			SymmetricMeter meter = manager.getComponent(meterId);
			Value<Integer> frequencyValue = meter.getFrequency();
			int frequency = frequencyValue.orElse(0); // voltage is in mV
			ret = frequency / 1000.0f;
		} catch (OpenemsNamedException e) {
			System.out.println(e);
		}
		return ret;
	}

	protected void setSyncBridge(boolean b) {
		System.out.println("setSyncBridge : parameter --> " + b);
		try {
			System.out.println("Try to write " + b + " to the sync bridge channel");
			System.out.println("output sync bridge address: " + outputSyncBridge);
			BooleanWriteChannel outputSyncDeviceBridgeChannel = this.manager
					.getChannel(ChannelAddress.fromString(outputSyncBridge));
			outputSyncDeviceBridgeChannel.setNextWriteValue(b);
		} catch (IllegalArgumentException | OpenemsNamedException e) {
			System.out.println("Error writing channel");
			System.out.println(e.getMessage());
		}
	}

	protected boolean isSystemOngrid() {
		if (isDigitalInputDefined(inputNA1) && isDigitalInputDefined(inputNA2)) {
			try {
				boolean na1Value = getBooleanReadChannel(inputNA1).value().get();
				boolean na2Value = getBooleanReadChannel(inputNA2).value().get();

				if (na1Inverted) {
					na1Value = !na1Value;
				}

				if (na2Inverted) {
					na2Value = !na2Value;
				}

				return na1Value && na2Value;
			} catch (IllegalArgumentException | OpenemsNamedException e) {
				return false;
			}
		}
		return false;
	}

	protected boolean isSystemOffgrid() {
		if (isDigitalInputDefined(inputNA1) && isDigitalInputDefined(inputNA2)) {
			try {
				boolean na1Value = getBooleanReadChannel(inputNA1).value().get();
				boolean na2Value = getBooleanReadChannel(inputNA2).value().get();

				if (na1Inverted) {
					na1Value = !na1Value;
				}

				if (na2Inverted) {
					na2Value = !na2Value;
				}

				return !na1Value || !na2Value;
			} catch (IllegalArgumentException | OpenemsNamedException e) {
				return false;
			}
		}
		return false;
	}

	protected void startBatteries() {
		Helper.startBattery(getBattery1());
		Helper.startBattery(getBattery2());
		Helper.startBattery(getBattery3());
	}

	protected boolean isBatteriesStarted() {
		boolean running = true;
		if (getBattery1() != null) {
			running = running && Helper.isRunning(getBattery1());
		}
		if (getBattery2() != null) {
			running = running && Helper.isRunning(getBattery2());
		}
		if (getBattery3() != null) {
			running = running && Helper.isRunning(getBattery3());
		}
		return running;
	}
	
	
	protected void setStringControlMode() {
		int weightingMode = WeightingHelper.getStringControlMode(getBattery1(), getBattery2(), getBattery3());
		getGridconPcs().setStringControlMode(weightingMode);
	}

	protected void setStringWeighting() {
		float activePower = getGridconPcs().getActivePower();

		Float[] weightings = WeightingHelper.getWeighting(activePower, getBattery1(), getBattery2(), getBattery3());

		getGridconPcs().setWeightStringA(weightings[0]);
		getGridconPcs().setWeightStringB(weightings[1]);
		getGridconPcs().setWeightStringC(weightings[2]);

	}

	protected void setDateAndTime() {
		int date = this.convertToInteger(this.generateDate(LocalDateTime.now()));
		getGridconPcs().setSyncDate(date);
		int time = this.convertToInteger(this.generateTime(LocalDateTime.now()));
		getGridconPcs().setSyncTime(time);
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

	GridconPcs getGridconPcs() {
		return getComponent(gridconPcsId);
	}

	Battery getBattery1() {
		return getComponent(battery1Id);
	}

	Battery getBattery2() {
		return getComponent(battery2Id);
	}

	Battery getBattery3() {
		return getComponent(battery3Id);
	}

	<T> T getComponent(String id) {
		T component = null;
		try {
			component = manager.getComponent(id);
		} catch (OpenemsNamedException e) {
			System.out.println(e);
		}
		return component;
	}

	@Override
	public IState getStateBefore() {
		return stateBefore;
	}

	@Override
	public void setStateBefore(IState stateBefore) {
		if (this.stateBefore == null || !this.stateBefore.equals(stateBefore)) {
			this.stateBefore = stateBefore;
		}
	}

	@Override
	public void setSubStateObject(StateObject subStateObject) {
		this.subStateObject = subStateObject;
	}

	@Override
	public StateObject getSubStateObject() {
		return subStateObject;
	}
}
