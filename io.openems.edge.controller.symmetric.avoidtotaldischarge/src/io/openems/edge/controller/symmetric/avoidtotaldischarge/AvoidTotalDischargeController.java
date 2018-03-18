package io.openems.edge.controller.symmetric.avoidtotaldischarge;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashSet;
import java.util.Set;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.api.controller.ControllerInterface;
import io.openems.edge.api.controller.ControllerState;
import io.openems.edge.api.device.ess.SymmetricEssInterface;
import io.openems.edge.api.message.Message;

@Component(service = { ControllerInterface.class }, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = AvoidTotalDischargeController.Config.class, factory = true)
public class AvoidTotalDischargeController implements ControllerInterface {

	@ObjectClassDefinition(name = "AvoidTotalDischargeController Configuration")
	@interface Config {
		String id();

		int minSoc() default 5;

		int chargeSoc() default 2;

		int maxSoc() default 95;

		int minSocHysteresis() default 2;

		boolean enableDischarge() default false;

		String dischargePeriod() default "P4W";

		String ess_target();
	}

	Config config;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	public SymmetricEssInterface ess;
	private State state;
	private LocalDate nextDischargeDate;
	private Period dischargePeriod;
	private ControllerState controllerState = ControllerState.INIT;
	private Set<Message> messages = new HashSet<>();

	protected enum State {
		CHARGESOC, MINSOC, NORMAL, FULL, EMPTY
	}

	@Activate
	public void activate(Config config) {
		this.config = config;
		dischargePeriod = Period.parse(config.dischargePeriod());
	}

	@Modified
	public void modified(Config config) {
		this.config = config;
		dischargePeriod = Period.parse(config.dischargePeriod());
	}

	@Override
	public ControllerState getStatus() {
		return controllerState;
	}

	@Override
	public void executeLogic() {
		/*
		 * Calculate SetActivePower according to MinSoc
		 */
		controllerState = ControllerState.WORKING;
		switch (state) {
		case CHARGESOC:
			if (ess.getSoc().get() > config.minSoc()) {
				state = State.MINSOC;
			} else {
				// TODO power impelementation
				// try {
				// long maxChargePower = Math.max(ess.power.getMinP().orElse(0L),
				// Math.abs(ess.maxNominalPower.valueOptional().orElse(10000L) / 5) * -1);
				// ess.maxActivePowerLimit.setP(maxChargePower);
				// ess.power.applyLimitation(ess.maxActivePowerLimit);
				// } catch (PowerException e) {
				// log.error("Failed to set Power!", e);
				// }
			}
			break;
		case MINSOC:
			if (ess.getSoc().get() < config.chargeSoc()) {
				state = State.CHARGESOC;
			} else if (ess.getSoc().get() >= config.minSoc() + config.minSocHysteresis()) {
				state = State.NORMAL;
			} else if (nextDischargeDate != null && nextDischargeDate.equals(LocalDate.now())
					&& config.enableDischarge()) {
				state = State.EMPTY;
			} else {
				// TODO power impelementation
				// ess.maxActivePowerLimit.setP(0L);
				// try {
				// ess.power.applyLimitation(ess.maxActivePowerLimit);
				// } catch (PowerException e) {
				// log.error("Failed to set Power!", e);
				// }
			}
			break;
		case NORMAL:
			if (ess.getSoc().get() <= config.minSoc()) {
				state = State.MINSOC;
			} else if (ess.isFull()) {
				state = State.FULL;
			}
			break;
		case FULL:
			// TODO power impelementation
			// ess.minActivePowerLimit.setP(0L);
			// try {
			// ess.power.applyLimitation(ess.minActivePowerLimit);
			// } catch (PowerException e) {
			// log.error("Failed to set Power!", e);
			// }
			if (ess.getSoc().get() < config.maxSoc()) {
				state = State.NORMAL;
			}
			break;
		case EMPTY:
			if (ess.isEmpty()) {
				// Ess is Empty set Date and charge to minSoc
				addPeriod();
				state = State.CHARGESOC;
			}
			break;
		default:
			controllerState = ControllerState.ERROR;
			break;
		}
	}

	private void addPeriod() {
		if (this.nextDischargeDate != null) {
			this.nextDischargeDate = this.nextDischargeDate.plus(dischargePeriod);
			// TODO persist last discharge date
			// nextDischarge.updateValue(this.nextDischargeDate.toString(), true);
		}
	}

	@Override
	public String getId() {
		return config.id();
	}

	@Override
	public Set<Message> getControllerMessages() {
		return messages;
	}

}
