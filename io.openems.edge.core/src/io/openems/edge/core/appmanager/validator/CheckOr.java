package io.openems.edge.core.appmanager.validator;

import java.util.Map;
import java.util.Objects;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.validator.CheckableFactory.ClosableCheckable;
import io.openems.edge.core.appmanager.validator.ValidatorConfig.CheckableConfig;

@Component(//
		name = CheckOr.COMPONENT_NAME, //
		scope = ServiceScope.PROTOTYPE //
)
public class CheckOr extends AbstractCheckable implements Checkable {

	public static final String COMPONENT_NAME = "Validator.Checkable.CheckOr";

	private final Logger log = LoggerFactory.getLogger(CheckOr.class);

	private final CheckableFactory checkableFactory;

	private CheckableConfig check1Config;
	private ClosableCheckable check1;
	private CheckableConfig check2Config;
	private ClosableCheckable check2;

	@Activate
	public CheckOr(//
			ComponentContext componentContext, //
			@Reference CheckableFactory checkableFactory //
	) {
		super(componentContext);
		this.checkableFactory = checkableFactory;
	}

	@Deactivate
	private void deactivate() {
		try {
			this.check1.close();
		} catch (Exception e) {
			this.log.error("Unable to close checkable " + this.check1Config.checkableComponentName(), e);
		}
		try {
			this.check2.close();
		} catch (Exception e) {
			this.log.error("Unable to close checkable " + this.check2Config.checkableComponentName(), e);
		}
	}

	@Override
	public void setProperties(Map<String, ?> properties) {
		this.check1Config = Objects.requireNonNull((CheckableConfig) properties.get("check1"),
				"First check must not be null");
		this.check2Config = Objects.requireNonNull((CheckableConfig) properties.get("check2"),
				"Second check must not be null");
	}

	@Override
	public boolean check() {
		this.check1 = this.checkableFactory.useCheckable(this.check1Config.checkableComponentName());
		this.check1.setProperties(this.check1Config.properties());
		this.check2 = this.checkableFactory.useCheckable(this.check2Config.checkableComponentName());
		this.check2.setProperties(this.check2Config.properties());

		final var check1Failed = this.check1.check() == this.check1Config.invertResult();
		final var check2Failed = this.check2.check() == this.check2Config.invertResult();
		return !(check1Failed && check2Failed);
	}

	@Override
	public String getErrorMessage(Language language) {
		return AbstractCheckable.getTranslation(language, "Validator.Checkable.CheckOr.Message",
				this.getCheck1ErrorMessage(language), this.getCheck2ErrorMessage(language));
	}

	@Override
	public String getInvertedErrorMessage(Language language) {
		throw new UnsupportedOperationException();
	}

	private String getCheck1ErrorMessage(Language language) {
		return this.check1Config.invertResult() //
				? this.check1.getInvertedErrorMessage(language)
				: this.check1.getErrorMessage(language);
	}

	private String getCheck2ErrorMessage(Language language) {
		return this.check2Config.invertResult() //
				? this.check2.getInvertedErrorMessage(language)
				: this.check2.getErrorMessage(language);
	}

}
