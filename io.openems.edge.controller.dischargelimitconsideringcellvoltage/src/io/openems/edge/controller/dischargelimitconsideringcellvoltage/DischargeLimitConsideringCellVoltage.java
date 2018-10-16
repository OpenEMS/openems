package io.openems.edge.controller.dischargelimitconsideringcellvoltage;


import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.OptionsEnum;
import io.openems.edge.common.component.AbstractOpenemsComponent;
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
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;
	
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private Battery battery;

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
		log.debug("DischargeLimitConsideringCellVoltage.run()");
		
		Map<String, Float> values = getValuesFromChannels();
		checkState(values);
		handleStateMachine(values);

	}
	private void handleStateMachine(Map<String, Float> values) {
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
		this.status = status;
		this.channel(ChannelId.STATE_MACHINE).setNextValue(this.status);		
	}

	private void doPendingHandling(Map<String, Float> values) {
		if (values.get(KEY_MIN_CELL_VOLTAGE) > minCellVoltage) {
			this.setStatus(State.NORMAL);
			timeSinceMinCellVoltageWasBelowLimit = null;
		} else {
			if (timeSinceMinCellVoltageWasBelowLimit.plusSeconds(timeUntilChargeIsForced).isBefore(LocalDateTime.now())) {
				startCharging();
			}
		}
	}


	private void doNormalHandling(Map<String, Float> values) {		
		if (values.get(KEY_SYSTEM_VOLTAGE) < minimalSystemVoltage || values.get(KEY_MIN_CELL_VOLTAGE) < absolutMinCellVoltage || values.get(KEY_SOC) < chargeSoC) {
			startCharging();
			return;
		}
		if ( values.get(KEY_MIN_CELL_VOLTAGE) < minCellVoltage &&  values.get(KEY_MIN_CELL_VOLTAGE) > absolutMinCellVoltage) {
			setPending();
			return;
		}
		if (values.get(KEY_SOC) < minSoC) {
			forbidDischarging();
			return;
		}
	}

	private void forbidDischarging() {
		try {
			this.ess.addPowerConstraintAndValidate("DischargeLimitConsideringCellVoltage", Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS, 0);
		} catch (PowerException e) {
			log.error("Exception occurred in DischargeLimitConsideringCellVoltage.forbidCharging()\n" + e.getMessage());
		}		
	}

	private void setPending() {
		this.setStatus(State.PENDING);
		timeSinceMinCellVoltageWasBelowLimit = LocalDateTime.now(); 
	}

	private void startCharging() {
		log.debug("DischargeLimitConsideringCellVoltage.startCharging()");
		timeSinceMinCellVoltageWasBelowLimit = null;
		int maxCharge = this.ess.getPower().getMinPower(ess, Phase.ALL, Pwr.ACTIVE);
        int calculatedPower = maxCharge / 5;
		try {
			this.ess.addPowerConstraintAndValidate("DischargeLimitConsideringCellVoltage", Phase.ALL, Pwr.ACTIVE,
					Relationship.LESS_OR_EQUALS, calculatedPower);
			this.setStatus(State.CHARGING);
		} catch (PowerException e) {
			this.logError(this.log, e.getMessage());
		}
	}

	private void doChargeHandling(Map<String, Float> values) {
		// if total voltage is above limit, cell voltage is above limit and soc is above limit we can stop charging
		//otherwise we continue charging
		if (values.get(KEY_SYSTEM_VOLTAGE) > minimalSystemVoltage && values.get(KEY_MIN_CELL_VOLTAGE) > minCellVoltage && values.get(KEY_SOC) > chargeSoC) {
			stopCharging();
		}		
	}

	private void stopCharging() {
		this.setStatus(State.NORMAL);	
	}

	private void checkState(Map<String, Float> values) {
		if (this.getStatus() == State.INITIALIZING && areAllValuesInMapSet(values)) {
			this.setStatus(State.NORMAL);
		} else if (this.getStatus() != State.INITIALIZING && !areAllValuesInMapSet(values)) {
			this.setStatus(State.NO_VALUES_PRESENT);
		}  else if (this.getStatus() == State.NO_VALUES_PRESENT && areAllValuesInMapSet(values)) {
			this.setStatus(State.NORMAL);
		}
	}

	private boolean areAllValuesInMapSet(Map<String, Float> values) {
		// if value is Float.MinValue this means that this value is not set
		for (Float value : values.values()) {
			if (value == Float.MIN_VALUE) {
				return false;
			}
		}
		return true;
	}

	private Map<String, Float> getValuesFromChannels() {
		Optional<Integer> vOpt = battery.getVoltage().getNextValue().asOptional();
		Optional<Integer> mcvOpt = battery.getMinimalCellVoltage().getNextValue().asOptional();
		Optional<Integer> socOpt = battery.getSoc().getNextValue().asOptional();
		
		Map<String, Float> values = new HashMap<>();
		
		putValueIntoMap(values, vOpt, KEY_SYSTEM_VOLTAGE, 1);
		putValueIntoMap(values, mcvOpt, KEY_MIN_CELL_VOLTAGE, 0.001f);
		putValueIntoMap(values, socOpt, KEY_SOC, 1);
		
		return values;
	}

	private void putValueIntoMap(Map<String, Float> values, Optional<Integer> valueOpt, String key, float factor) {
		if (valueOpt != null && valueOpt.isPresent()) {
			values.put(key, ( valueOpt.get() * factor ));
		} else {
			values.put(key, Float.MIN_VALUE);
		}		
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		
		// update filter for 'ess'
		if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "ess", config.ess_id())) {
			return;
		}
		
		// update filter for 'battery'
		if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "battery", config.battery_id())) {
			return;
		}

		// Write data from configuration into field variables
		writeDataFromConfigIntoFields(config);
		
		this.setStatus(State.INITIALIZING);
	}

	private void writeDataFromConfigIntoFields(Config config) {
		minCellVoltage = config.firstCellVoltageLimit();
		absolutMinCellVoltage = config.secondCellVoltageLimit();
		minimalSystemVoltage = config.minimalTotalVoltage();
		chargeSoC = config.ChargeSoc();
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
	
	public enum State implements OptionsEnum {
		UNDEFINED(0, "Undefined"),
		INITIALIZING(1, "Initializing"),
		NO_VALUES_PRESENT(2, "No values present"),
		NORMAL(3, "Normal"),
		PENDING(4, "Pending"),
		CHARGING(5, "Charging"), 
		;

		@Override
		public int getValue() {
			return this.value;
		}

		@Override
		public String getOption() {
			return this.option;
		}
		
		State(int value, String option) {
			this.value = value;
			this.option = option;
		}
		
		private int value;
		private String option;		
	}

}
