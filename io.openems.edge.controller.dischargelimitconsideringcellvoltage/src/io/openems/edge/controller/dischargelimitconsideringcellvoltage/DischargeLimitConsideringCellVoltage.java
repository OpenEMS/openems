package io.openems.edge.controller.dischargelimitconsideringcellvoltage;

import java.time.LocalDateTime;
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

import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.OptionsEnum;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.PowerException;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

/**
 * 
 * This controller charges a battery to a given state of charge if 
 *  - the system voltage goes below a specified value
 *  - the minimal cell voltage goes below a specified value
 *  - the minimal cell voltage goes below a specified value within a certain time period 
 *
 */
@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Controller.DischargeLimitConsideringCellVoltage", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class DischargeLimitConsideringCellVoltage extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(DischargeLimitConsideringCellVoltage.class);

	@Reference
	protected ComponentManager componentManager;

	private String essId;
	private String batteryId;

	private State status = State.UNDEFINED;
	private float minCellVoltage;
	private float absolutMinCellVoltage;
	private float minimalSystemVoltage;
	private float chargeSoC;
	private float minSoC;
	private int timeUntilChargeIsForced;
	private LocalDateTime timeSinceMinCellVoltageWasBelowLimit;

	
	private static String KEY_SYSTEM_VOLTAGE = "KEY_SYSTEM_VOLTAGE";
	private static String KEY_MIN_CELL_VOLTAGE = "KEY_MIN_CELL_VOLTAGE";
	private static String KEY_SOC = "KEY_SOC";
	
	public DischargeLimitConsideringCellVoltage() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}
	
	@Override
	public void run() {
		debug("DischargeLimitConsideringCellVoltage.run()");
		
		Map<String, Float> values = getValuesFromChannels();
		checkState(values);
		handleStateMachine(values);

	}
	private void handleStateMachine(Map<String, Float> values) {
		debug("DischargeLimitConsideringCellVoltage.handleStateMachine()");
		switch(getStatus()) {
		case CHARGING:
			doChargeHandling(values);
			break;
		case NORMAL:
			doNormalHandling(values);
			break;
		case PENDING:
			doPendingHandling(values);
			break;
		case NO_VALUES_PRESENT:
		case INITIALIZING:
		case UNDEFINED:
			// not possible to do anything
			break;
		}
		
	}

	public State getStatus() {
		return this.status;
	}

	private void setStatus(State status) {
		debug("DischargeLimitConsideringCellVoltage.setStatus()");
		this.status = status;
		this.channel(ChannelId.STATE_MACHINE).setNextValue(this.status);		
	}

	private void doPendingHandling(Map<String, Float> values) {
		debug("DischargeLimitConsideringCellVoltage.doPendingHandling()");
		if (values.get(KEY_MIN_CELL_VOLTAGE) > minCellVoltage) {
			debug("Min cell voltage is higher than limit --> " + values.get(KEY_MIN_CELL_VOLTAGE) + " > " + minCellVoltage);
			this.setStatus(State.NORMAL);
			timeSinceMinCellVoltageWasBelowLimit = null;
		} else {
			if (timeSinceMinCellVoltageWasBelowLimit.plusSeconds(timeUntilChargeIsForced).isBefore(LocalDateTime.now())) {
				debug("time has elapsed, start charging");
				startCharging();
			}
		}
	}

	private void debug(String message) {
		log.debug(message);
	}
	
	private void error(String message) {
		log.error(message);
	}

	@Override
	public String debugLog() {		
		return "[" + this.id() + " state: " + getStatus() + "]"; 
	}

	private void doNormalHandling(Map<String, Float> values) {
		debug("DischargeLimitConsideringCellVoltage.doNormalHandling()");
		if (values.get(KEY_SYSTEM_VOLTAGE) < minimalSystemVoltage || values.get(KEY_MIN_CELL_VOLTAGE) < absolutMinCellVoltage || values.get(KEY_SOC) < chargeSoC) {
			if (values.get(KEY_SYSTEM_VOLTAGE) < minimalSystemVoltage) {
				debug("System voltage too low --> start charging");
			}
			if ( values.get(KEY_MIN_CELL_VOLTAGE) < absolutMinCellVoltage) {
				debug("Min cell voltage too low --> start charging");
			}
			if (values.get(KEY_SOC) < chargeSoC) {
				debug("SoC too low --> start charging");
			}
			startCharging();
			return;
		}
		if ( values.get(KEY_MIN_CELL_VOLTAGE) < minCellVoltage && values.get(KEY_MIN_CELL_VOLTAGE) > absolutMinCellVoltage) {
			debug("Min cell voltage is in range --> set pending");
			setPending();
			return;
		}
		if (values.get(KEY_SOC) < minSoC) {
			debug("SoC < MinSoC --> forbid further discharging");
			forbidDischarging();
			return;
		}
	}

	private void forbidDischarging() {
		debug("DischargeLimitConsideringCellVoltage.forbidDischarging()");
		try {
			this.getEss().addPowerConstraintAndValidate("DischargeLimitConsideringCellVoltage", Phase.ALL, Pwr.ACTIVE,
					Relationship.LESS_OR_EQUALS, 0);
		} catch (PowerException e) {
			error("Exception occurred in DischargeLimitConsideringCellVoltage.forbidCharging()\n" + e.getMessage());
		}		
	}

	private void setPending() {
		debug("DischargeLimitConsideringCellVoltage.setPending()");
		this.setStatus(State.PENDING);
		timeSinceMinCellVoltageWasBelowLimit = LocalDateTime.now(); 
	}

	private void startCharging() {
		debug("DischargeLimitConsideringCellVoltage.startCharging()");
		ManagedSymmetricEss ess = this.getEss();
		timeSinceMinCellVoltageWasBelowLimit = null;
		int maxCharge = ess.getPower().getMinPower(ess, Phase.ALL, Pwr.ACTIVE);
        int calculatedPower = maxCharge / 5;
        debug("Calculated power: " + calculatedPower);
		try {
			ess.addPowerConstraintAndValidate("DischargeLimitConsideringCellVoltage", Phase.ALL, Pwr.ACTIVE,
					Relationship.LESS_OR_EQUALS, calculatedPower);
			this.setStatus(State.CHARGING);
		} catch (PowerException e) {
			error("Error occurred while setting charge constraint\n" + e.getMessage());
		}
	}

	private void doChargeHandling(Map<String, Float> values) {
		debug("DischargeLimitConsideringCellVoltage.doChargeHandling()");
		// if total voltage is above limit, cell voltage is above limit and soc is above limit we can stop charging
		//otherwise we continue charging
		if (values.get(KEY_SYSTEM_VOLTAGE) > minimalSystemVoltage && values.get(KEY_MIN_CELL_VOLTAGE) > minCellVoltage && values.get(KEY_SOC) > chargeSoC) {			
			debug("Limits are reached --> stop charging");
			stopCharging();
		}		
	}

	private void stopCharging() {
		debug("DischargeLimitConsideringCellVoltage.stopCharging()");
		this.setStatus(State.NORMAL);	
	}

	private void checkState(Map<String, Float> values) {
		debug("DischargeLimitConsideringCellVoltage.checkState()");
		if (this.getStatus() == State.INITIALIZING && areAllValuesInMapSet(values)) {
			this.setStatus(State.NORMAL);
		} else if (this.getStatus() != State.INITIALIZING && !areAllValuesInMapSet(values)) {
			this.setStatus(State.NO_VALUES_PRESENT);
		}  else if (this.getStatus() == State.NO_VALUES_PRESENT && areAllValuesInMapSet(values)) {
			this.setStatus(State.NORMAL);
		}
	}

	private boolean areAllValuesInMapSet(Map<String, Float> values) {
		debug("DischargeLimitConsideringCellVoltage.areAllValuesInMapSet()");
		// if value is Float.MinValue this means that this value is not set
		for (Float value : values.values()) {
			if (value == Float.MIN_VALUE) {
				return false;
			}
		}
		return true;
	}

	private Map<String, Float> getValuesFromChannels() {
		debug("DischargeLimitConsideringCellVoltage.getValuesFromChannels()");
		Battery battery = this.getBattery();
		Optional<Integer> vOpt = battery.getVoltage().getNextValue().asOptional();
		Optional<Integer> mcvOpt = battery.getMinCellVoltage().getNextValue().asOptional();
		Optional<Integer> socOpt = battery.getSoc().getNextValue().asOptional();
		
		Map<String, Float> values = new HashMap<>();
		
		putValueIntoMap(values, vOpt, KEY_SYSTEM_VOLTAGE, 1);
		putValueIntoMap(values, mcvOpt, KEY_MIN_CELL_VOLTAGE, 0.001f);
		putValueIntoMap(values, socOpt, KEY_SOC, 1);
		
		return values;
	}

	private void putValueIntoMap(Map<String, Float> values, Optional<Integer> valueOpt, String key, float factor) {
		debug("DischargeLimitConsideringCellVoltage.putValueIntoMap()");
		if (valueOpt != null && valueOpt.isPresent()) {
			values.put(key, ( valueOpt.get() * factor ));
		} else {
			values.put(key, Float.MIN_VALUE);
		}		
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		debug("DischargeLimitConsideringCellVoltage.activate()");
		super.activate(context, config.id(), config.enabled());

		this.essId = config.ess_id();
		this.batteryId = config.battery_id();

		// Write data from configuration into field variables
		writeDataFromConfigIntoFields(config);
		
		this.setStatus(State.INITIALIZING);
	}

	private void writeDataFromConfigIntoFields(Config config) {
		debug("DischargeLimitConsideringCellVoltage.writeDataFromConfigIntoFields()");
		minCellVoltage = config.firstCellVoltageLimit();
		absolutMinCellVoltage = config.secondCellVoltageLimit();
		minimalSystemVoltage = config.minimalTotalVoltage();
		chargeSoC = config.chargeSoc();
		minSoC = config.minSoc();
		timeUntilChargeIsForced = config.timeSpan();
	}
	
	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		
		STATE_MACHINE(new Doc().level(Level.INFO).text("Current state").options(State.values())), //
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
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}
	

	private ManagedSymmetricEss getEss() {
		return this.componentManager.getComponent(this.essId);
	}

	private Battery getBattery() {
		return this.componentManager.getComponent(this.batteryId);
	}

	public enum State implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		INITIALIZING(1, "Initializing"), //
		NO_VALUES_PRESENT(2, "No values present"), //
		NORMAL(3, "Normal"), //
		PENDING(4, "Pending"), //
		CHARGING(5, "Charging");

		@Override
		public int getValue() {
			return this.value;
		}

		@Override
		public String getName() {
			return this.name;
		}
		
		State(int value, String name) {
			this.value = value;
			this.name = name;
		}
		
		private int value;
		private String name;		
	
		@Override
		public OptionsEnum getUndefined() {
			return UNDEFINED;
		}
	}

}
