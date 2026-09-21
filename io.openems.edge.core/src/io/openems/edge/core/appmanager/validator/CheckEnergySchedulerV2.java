package io.openems.edge.core.appmanager.validator;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.session.Language;
import io.openems.edge.energy.api.EnergyScheduler;
import io.openems.edge.energy.api.Version;

@Component(//
		name = CheckEnergySchedulerV2.COMPONENT_NAME, //
		scope = ServiceScope.PROTOTYPE //
)
public class CheckEnergySchedulerV2 extends AbstractCheckable implements Checkable {

	public static final String COMPONENT_NAME = "Validator.Checkable.CheckEnergySchedulerV2";

	private final EnergyScheduler energyScheduler;

	@Activate
	public CheckEnergySchedulerV2(//
			ComponentContext componentContext, //
			@Reference EnergyScheduler energyScheduler //
	) {
		super(componentContext);
		this.energyScheduler = energyScheduler;
	}

	@Override
	public boolean check() {
		return this.energyScheduler.getImplementationVersion() == Version.V2_ENERGY_SCHEDULABLE;
	}

	@Override
	public String getErrorMessage(Language language) {
		return AbstractCheckable.getTranslation(language, "Validator.Checkable.CheckEnergySchedulerV2.Message");
	}

	@Override
	public String getInvertedErrorMessage(Language language) {
		return AbstractCheckable.getTranslation(language,
				"Validator.Checkable.CheckEnergySchedulerV2.Message.Inverted");
	}
}
