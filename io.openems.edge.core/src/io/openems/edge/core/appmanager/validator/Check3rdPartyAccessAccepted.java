package io.openems.edge.core.appmanager.validator;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.meta.ThirdPartyUsageAcceptance;

@Component(//
		name = Check3rdPartyAccessAccepted.COMPONENT_NAME, //
		scope = ServiceScope.PROTOTYPE //
)
public class Check3rdPartyAccessAccepted extends AbstractCheckable implements Checkable {

	public static final String COMPONENT_NAME = "Validator.Checkable.Check3rdPartyAccessAccepted";

	private final Meta meta;

	@Activate
	public Check3rdPartyAccessAccepted(ComponentContext context, @Reference Meta meta) {
		super(context);
		this.meta = meta;
	}

	@Override
	public boolean check() {
		return this.meta.getThirdPartyUsageAcceptance() == ThirdPartyUsageAcceptance.ACCEPTED;
	}

	@Override
	public OpenemsNamedException getValidationError(Language language) {
		return OpenemsError.EDGE_APP_VALIDATION_CHECK_3RD_PARTY_ACCESS_ACCEPTED.exception(language);
	}

	@Override
	public OpenemsNamedException getInvertedValidationError(Language language) {
		throw new UnsupportedOperationException();
	}
}
