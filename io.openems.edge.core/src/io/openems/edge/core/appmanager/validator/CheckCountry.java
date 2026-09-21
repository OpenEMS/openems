package io.openems.edge.core.appmanager.validator;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.session.Language;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.meta.types.CountryCode;

@Component(//
		name = CheckCountry.COMPONENT_NAME, //
		scope = ServiceScope.PROTOTYPE //
)
public class CheckCountry extends AbstractCheckable implements Checkable {

	public static final String COMPONENT_NAME = "Validator.Checkable.CheckCountry";

	private final CountryCode country;

	private Set<CountryCode> allowedCountries = Collections.emptySet();

	@Activate
	public CheckCountry(//
			ComponentContext componentContext, //
			@Reference Meta meta //
	) {
		super(componentContext);
		this.country = meta.getCountryCode();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setProperties(Map<String, ?> properties) {
		this.allowedCountries = (Set<CountryCode>) properties.get("allowedCountries");
	}

	@Override
	public boolean check() {
		if (this.country == CountryCode.UNDEFINED) {
			return true;
		}
		return this.allowedCountries.contains(this.country);
	}

	@Override
	public String getErrorMessage(Language language) {
		return AbstractCheckable.getTranslation(language, "Validator.Checkable.CheckCountry.Message");
	}

	@Override
	public String getInvertedErrorMessage(Language language) {
		return AbstractCheckable.getTranslation(language, "Validator.Checkable.CheckCountry.Message");
	}
}
