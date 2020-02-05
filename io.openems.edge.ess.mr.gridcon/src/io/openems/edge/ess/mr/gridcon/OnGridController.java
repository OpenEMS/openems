package io.openems.edge.ess.mr.gridcon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.mr.gridcon.enums.GridConChannelId;

@Designate(ocd = OnGridConfig.class, factory = true)
@Component(//
		name = "Controller.Gridcon.OnGrid", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class OnGridController extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private GridconPCS gridconPCS;
	private Battery batteryA;
	private Battery batteryB;
	private Battery batteryC;

	StateMachine stateMachine;

	private final Logger log = LoggerFactory.getLogger(OnGridController.class);

	@Reference
	protected ComponentManager componentManager;

	public OnGridController() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, OnGridConfig config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.gridconPCS = componentManager.getComponent(config.gridcon_id());

		try {
			this.batteryA = componentManager.getComponent(config.bms_a_id());
		} catch (OpenemsNamedException e) {
			// if battery is null, no battery is connected on string a 
		}

		try {
			this.batteryB = componentManager.getComponent(config.bms_b_id());
		} catch (OpenemsNamedException e) {
			// if battery is null, no battery is connected on string b
		}

		try {
			this.batteryC = componentManager.getComponent(config.bms_c_id());
		} catch (OpenemsNamedException e) {
			// if battery is null, no battery is connected on string c
		}
		this.stateMachine = new StateMachine(gridconPCS, this);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		if (!this.isEnabled()) {
			return;
		}

		try {
			// prepare calculated Channels
			this.calculateGridMode();
			this.calculateBatteryData();
			this.calculateSoc();
			
			this.gridconPCS.weightingMap = getWeightingMap();
			this.gridconPCS.stringControlMode = getStringControlMode();

			// start state-machine handling
			this.stateMachine.run();

			this.gridconPCS.channel(GridConChannelId.STATE_CYCLE_ERROR).setNextValue(false);
		} catch (IllegalArgumentException | OpenemsNamedException e) {
			this.gridconPCS.channel(GridConChannelId.STATE_CYCLE_ERROR).setNextValue(true);
			this.logError(this.log, "State-Cycle Error: " + e.getMessage());
		}
	}

	private int getStringControlMode() {
		int weightingMode = 0; // Depends on number of battery strings!!!

		// --- // TODO if battery is not ready for work, remove it from the weighting
		// mode!!

		boolean useBatteryStringA = (batteryA != null && batteryA.getReadyForWorking().value().orElse(false));
		if (useBatteryStringA) {
			weightingMode += 1; // battA = 1 (2^0)
		}
		boolean useBatteryStringB = (batteryB != null && batteryB.getReadyForWorking().value().orElse(false));
		if (useBatteryStringB) {
			weightingMode += 8; // battB = 8 (2^3)
		}
		boolean useBatteryStringC = (batteryC != null && batteryC.getReadyForWorking().value().orElse(false));
		if (useBatteryStringC) {
			weightingMode += 64; // battC = 64 (2^6)
		}
		
		return weightingMode;
	}
	
	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //

		; //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
	
	/**
	 * Sets the weights for battery strings A, B and C according to max allowed
	 * current.
	 * 
	 * @return 
	 * @throws OpenemsNamedException
	 */
	private Map<String, Map<GridConChannelId, Float>> getWeightingMap() {
		
		Map<String, Map<GridConChannelId, Float>> ret = new HashMap<>();
		
		Map<GridConChannelId, Float> chargeMap = new HashMap<>();
		Map<GridConChannelId, Float> dischargeMap = new HashMap<>();
		Map<GridConChannelId, Float> zeroMap = new HashMap<>();
		
		ret.put(GridconPCS.KEY_POWER_CHARGING, chargeMap);
		ret.put(GridconPCS.KEY_POWER_DISCHARGING, dischargeMap);
		ret.put(GridconPCS.KEY_POWER_IS_ZERO, zeroMap);
		
			// Discharge
			if (batteryA != null && this.isBatteryReady(batteryA)) {
				float currentA = batteryA.getDischargeMaxCurrent().value().asOptional().orElse(0);
				float voltageA = batteryA.getVoltage().value().asOptional().orElse(0);
				dischargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A, currentA * voltageA);
			} else {
				dischargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A, 0f);
			}
			
			if (batteryB != null && this.isBatteryReady(batteryB)) {
				float currentB = batteryB.getDischargeMaxCurrent().value().asOptional().orElse(0);
				float voltageB = batteryB.getVoltage().value().asOptional().orElse(0);
				dischargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B, currentB * voltageB);
			} else {
				dischargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B, 0f);
			}
			
			if (batteryC != null && this.isBatteryReady(batteryC)) {
				float currentC = batteryC.getDischargeMaxCurrent().value().asOptional().orElse(0);
				float voltageC = batteryC.getVoltage().value().asOptional().orElse(0);
				dischargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C, currentC * voltageC);
			} else {
				dischargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C, 0f);
			}
			
			// Charge
			if (batteryA != null && this.isBatteryReady(batteryA)) {
				float currentA = batteryA.getChargeMaxCurrent().value().asOptional().orElse(0);
				float voltageA = batteryA.getVoltage().value().asOptional().orElse(0);
				chargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A, currentA * voltageA);
			} else {
				chargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A, 0f);
			}
			
			if (batteryB != null && this.isBatteryReady(batteryB)) {
				float currentB = batteryB.getChargeMaxCurrent().value().asOptional().orElse(0);
				float voltageB = batteryB.getVoltage().value().asOptional().orElse(0);
				chargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B, currentB * voltageB);
			} else {
				chargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B, 0f);
			}
				
			if (batteryC != null && this.isBatteryReady(batteryC)) {
				float currentC = batteryC.getChargeMaxCurrent().value().asOptional().orElse(0);
				float voltageC = batteryC.getVoltage().value().asOptional().orElse(0);
				chargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C, currentC * voltageC);
			} else {
				chargeMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C, 0f);
			}

			// active power is zero
			 
			
			float factor = 100;
			float weightA = 0;
			float weightB = 0;
			float weightC = 0;
			if (batteryA != null && batteryB != null && batteryC != null) { // ABC
				Optional<Integer> vAopt = batteryA.getVoltage().value().asOptional();
				Optional<Integer> vBopt = batteryB.getVoltage().value().asOptional();
				Optional<Integer> vCopt = batteryC.getVoltage().value().asOptional();
				
				// Racks die abgeschalten sind dürfen nicht berücksichtigt werden
				//--> Gewichtung auf 0
				
				if (vAopt.isPresent() && vBopt.isPresent() && vCopt.isPresent()) {
					float averageVoltageA = vAopt.get();
					float averageVoltageB = vBopt.get();
					float averageVoltageC = vCopt.get();
					
					float min = Math.min(averageVoltageA, Math.min(averageVoltageB, averageVoltageC));
					if (this.isBatteryReady(batteryA)) {
						weightA =  (averageVoltageA - min) * factor;
					}
					if (this.isBatteryReady(batteryB)) {
						weightB = (int) ((averageVoltageB - min) * factor);
					}
					if (this.isBatteryReady(batteryC)) {
						weightC = (int) ((averageVoltageC - min) * factor);
					}
				}
			} else if (batteryA != null && batteryB != null && batteryC == null) { // AB
				Optional<Integer> vAopt = batteryA.getVoltage().value().asOptional();
				Optional<Integer> vBopt = batteryB.getVoltage().value().asOptional();
				if (vAopt.isPresent() && vBopt.isPresent()) {
					float averageVoltageA = vAopt.get();
					float averageVoltageB = vBopt.get();
					float min = Math.min(averageVoltageA, averageVoltageB);
					if (this.isBatteryReady(batteryA)) {
						weightA = (averageVoltageA - min) * factor;
					}
					if (this.isBatteryReady(batteryB)) {
						weightB = (averageVoltageB - min) * factor;
					}
				}
			} else if (batteryA != null && batteryB == null && batteryC != null) { // AC
				Optional<Integer> vAopt = batteryA.getVoltage().value().asOptional();
				Optional<Integer> vCopt = batteryC.getVoltage().value().asOptional();
				if (vAopt.isPresent() && vCopt.isPresent()) {
					float averageVoltageA = vAopt.get();
					float averageVoltageC = vCopt.get();
					float min = Math.min(averageVoltageA, averageVoltageC);
					if (this.isBatteryReady(batteryA)) {
						weightA = (averageVoltageA - min) * factor;
					}
					if (this.isBatteryReady(batteryC)) {
						weightC = (averageVoltageC - min) * factor;
					}
				}
			} else if (batteryA == null && batteryB != null && batteryC != null) { // BC
				Optional<Integer> vBopt = batteryB.getVoltage().value().asOptional();
				Optional<Integer> vCopt = batteryC.getVoltage().value().asOptional();
				if (vBopt.isPresent() && vCopt.isPresent()) {
					float averageVoltageB = vBopt.get();
					float averageVoltageC = vCopt.get();
					float min = Math.min(averageVoltageB, averageVoltageC);
					if (this.isBatteryReady(batteryB)) {
						weightB = (averageVoltageB - min) * factor;
					}
					if (this.isBatteryReady(batteryC)) {
						weightC = (averageVoltageC - min) * factor;
					}
				}
			}
			
			zeroMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A, weightA);
			zeroMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B, weightB);
			zeroMap.put(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C, weightC);
		
		
		return ret;
	}

	private boolean isBatteryReady(Battery battery) {
		if (battery == null) {
			return false;
		}
		
		Optional<Boolean> readyOpt = battery.getReadyForWorking().getNextValue().asOptional();
		
		return (readyOpt.isPresent() && readyOpt.get());
	}

	/**
	 * Handles Battery data, i.e. setting allowed charge/discharge power.
	 */
	void calculateBatteryData() {
		int allowedCharge = 0;
		int allowedDischarge = 0;
		int capacity = 0;

		for (Battery battery : getBatteries()) {
			allowedCharge += battery.getVoltage().value().orElse(0) * battery.getChargeMaxCurrent().value().orElse(0)
					* -1;
			allowedDischarge += battery.getVoltage().value().orElse(0)
					* battery.getDischargeMaxCurrent().value().orElse(0);
			capacity += battery.getCapacity().value().orElse(0);
		}
		this.gridconPCS.getAllowedCharge().setNextValue(allowedCharge);
		this.gridconPCS.getAllowedDischarge().setNextValue(allowedDischarge);
		this.gridconPCS.getCapacity().setNextValue(capacity);
	}

	public boolean isAtLeastOneBatteryReady() {
		for (Battery battery : getBatteries()) {
			Optional<Boolean> val = battery.getReadyForWorking().value().asOptional();

			if (val.isPresent() && val.get()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Evaluates the Grid-Mode and sets the GRID_MODE channel accordingly.
	 * 
	 * @return
	 * 
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */
	void calculateGridMode() throws IllegalArgumentException, OpenemsNamedException {
		GridMode gridMode = GridMode.ON_GRID;
		this.gridconPCS.getGridMode().setNextValue(gridMode);
	}

	/**
	 * Calculates the State-of-charge of all Batteries; if all batteries are
	 * available. Otherwise sets UNDEFINED.
	 */
	void calculateSoc() {
		float sumTotalCapacity = 0;
		float sumCurrentCapacity = 0;
		for (Battery b : this.getBatteries()) {
			Optional<Integer> totalCapacityOpt = b.getCapacity().value().asOptional();
			Optional<Integer> socOpt = b.getSoc().value().asOptional();
			if (!totalCapacityOpt.isPresent() || !socOpt.isPresent()) {
				// if at least one Battery has no valid value -> set UNDEFINED
				this.gridconPCS.getSoc().setNextValue(null);
				return;
			}
			int totalCapacity = totalCapacityOpt.get();
			int soc = socOpt.get();
			sumTotalCapacity += totalCapacity;
			sumCurrentCapacity += totalCapacity * soc / 100.0;
		}
		int soc = Math.round(sumCurrentCapacity * 100 / sumTotalCapacity);
		this.gridconPCS.getSoc().setNextValue(soc);
	}

	/**
	 * Gets all Batteries.
	 * 
	 * @return a collection of Batteries; guaranteed to be not-null.
	 */
	private Collection<Battery> getBatteries() {

		Collection<Battery> batteries = new ArrayList<>();
		if (batteryA != null) {
			batteries.add(batteryA);
		}

		if (batteryB != null) {
			batteries.add(batteryB);
		}

		if (batteryC != null) {
			batteries.add(batteryC);
		}

		return batteries;
	}

	public enum State implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		RUNNING(0, "Rubnning"), //
		;

		private final int value;
		private final String name;

		private State(int value, String name) {
			this.value = value;
			this.name = name;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public OptionsEnum getUndefined() {
			return UNDEFINED;
		}
	}
}
