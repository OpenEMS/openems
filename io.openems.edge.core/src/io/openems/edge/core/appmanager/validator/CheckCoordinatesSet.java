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

@Component(//
		name = CheckCoordinatesSet.COMPONENT_NAME, //
		scope = ServiceScope.PROTOTYPE //
)
public class CheckCoordinatesSet extends AbstractCheckable implements Checkable {

	public static final String COMPONENT_NAME = "Validator.Checkable.CheckCoordinatesSet";

	private final Meta meta;

	@Activate
	public CheckCoordinatesSet(ComponentContext context, @Reference Meta meta) {
		super(context);
		this.meta = meta;
	}

	@Override
	public boolean check() {
		return this.meta.getCoordinates() != null;
	}

	@Override
	public OpenemsNamedException getValidationError(Language language) {
		return OpenemsError.EDGE_APP_VALIDATION_CHECK_COORDINATES_SET.exception(language);
	}

	@Override
	public OpenemsNamedException getInvertedValidationError(Language language) {
		throw new UnsupportedOperationException();
	}
}
