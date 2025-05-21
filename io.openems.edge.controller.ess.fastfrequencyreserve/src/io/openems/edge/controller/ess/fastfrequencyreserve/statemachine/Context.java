package io.openems.edge.controller.ess.fastfrequencyreserve.statemachine;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.controller.ess.fastfrequencyreserve.ControllerFastFrequencyReserve;
import io.openems.edge.controller.ess.fastfrequencyreserve.ControllerFastFrequencyReserveImpl;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.ActivationTime;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.SupportDuration;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.ElectricityMeter;

public class Context extends AbstractContext<ControllerFastFrequencyReserveImpl> {

	protected final Clock clock;
	protected final int dischargePower;
	protected final long startTimestamp;
	protected final int duration;
	protected final int freqLimit;
	protected final ActivationTime activationRunTime;
	protected final SupportDuration supportDuration;
	protected final ControllerFastFrequencyReserve parentController;
	protected final ManagedSymmetricEss ess;
	protected final ElectricityMeter meter;

	protected static Instant _cycleStart;

	public Context(ControllerFastFrequencyReserve fastFrequencyReserve, //
			Clock clock, //
			ManagedSymmetricEss ess, //
			ElectricityMeter meter, //
			long startTimestamp, //
			int duration, //
			int dischargePower, //
			int freqLimit, //
			ActivationTime activationRunTime, //
			SupportDuration supportDuration) {
		this.clock = clock;
		this.parentController = fastFrequencyReserve;
		this.startTimestamp = startTimestamp;
		this.duration = duration;
		this.dischargePower = dischargePower;
		this.freqLimit = freqLimit;
		this.ess = ess;
		this.meter = meter;
		this.activationRunTime = activationRunTime;
		this.supportDuration = supportDuration;
	}

	public Instant getCycleStart() {
		return _cycleStart;
	}

	public void setCycleStart(Instant cycleStart) {
		LocalDateTime lastTriggered = LocalDateTime.ofInstant(cycleStart, ZoneId.systemDefault());
		String formattedDateTime = lastTriggered.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		this.parentController.setLastTriggeredTime(formattedDateTime);
		_cycleStart = cycleStart;
	}
}
