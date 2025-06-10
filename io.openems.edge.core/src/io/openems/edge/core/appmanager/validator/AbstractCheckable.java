package io.openems.edge.core.appmanager.validator;

import java.util.ResourceBundle;

import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;

import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.TranslationUtil;

public abstract class AbstractCheckable implements Checkable {

	protected final ComponentContext componentContext;

	public AbstractCheckable(ComponentContext componentContext) {
		this.componentContext = componentContext;
	}

	@Override
	public String getComponentName() {
		return this.componentContext.getProperties().get(ComponentConstants.COMPONENT_NAME).toString();
	}

	protected static String getTranslation(Language language, String key, Object... params) {
		if (language == null) {
			language = Language.DEFAULT;
		}
		// TODO translation
		switch (language) {
		case CZ:
		case ES:
		case FR:
		case NL:
			language = Language.EN;
			break;
		case DE:
		case EN:
			break;
		}

		var translationBundle = ResourceBundle.getBundle("io.openems.edge.core.appmanager.validator.translation",
				language.getLocal());
		return TranslationUtil.getTranslation(translationBundle, key, params);
	}

}
