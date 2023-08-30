package io.openems.edge.controller.ess.fastfrequencyreserve.statemachine;

import java.time.Instant;
import java.time.LocalDateTime;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.controller.ess.fastfrequencyreserve.FastFrequencyReserve;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.ActivationTime;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.SupportDuration;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine.State;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Context extends AbstractContext<FastFrequencyReserve> {

	private final Logger log = LoggerFactory.getLogger(Context.class);

	protected final int dischargePower; // = this.getDischargeActivePowerSetPoint().get();
	protected final long startTimestamp; // = this.getStartTimeStamp().get();
	protected final int duration; // = this.getDuration().get();
	protected final int freqLimit; // = this.getFrequencyLimitSetPoint().get();
	protected final FastFrequencyReserve parentController;
	protected final ComponentManager componentManager;
	protected final ManagedSymmetricEss ess;
	protected final ElectricityMeter meter;
	protected final ActivationTime activationRunTime;
	protected final SupportDuration supportDuration;

	private static LocalDateTime cycleStart;

	public Context(FastFrequencyReserve fastFrequencyReserve, ComponentManager componentManager,
			ManagedSymmetricEss ess, ElectricityMeter meter, long startTimestamp2, int duration2, int dischargePower2,
			int freqLimit2, ActivationTime activationRunTime, SupportDuration supportDuration) {
		super(fastFrequencyReserve);
		this.componentManager = componentManager;
		this.parentController = fastFrequencyReserve;
		this.startTimestamp = startTimestamp2;
		this.duration = duration2;
		this.dischargePower = dischargePower2;
		this.freqLimit = freqLimit2;
		this.ess = ess;
		this.meter = meter;
		this.activationRunTime = activationRunTime;
		this.supportDuration = supportDuration;
	}

	public LocalDateTime getCycleStart() {
		return cycleStart;
	}

	public void setCycleStart(LocalDateTime cycleS) {
		cycleStart = cycleS;
	}

	protected void setPowerandPrint(State state, int power, ManagedSymmetricEss e, ComponentManager componentManager)
			throws OpenemsNamedException {

		componentManager.getClock().instant();
		super.logInfo(this.log, "Set power : " + power //
				+ " W| Time stamp :  " + Instant.now() //
				+ "| State : " + state);
		e.setActivePowerEquals(power);
	}

}
