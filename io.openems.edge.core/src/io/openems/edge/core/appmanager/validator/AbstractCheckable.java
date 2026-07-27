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
		final var availableLanguage = switch (language) {
		// Language was not set -> fall back to default (currently GERMAN)
		case null -> Language.DEFAULT;
		// Translations are not available -> fall back to ENGLISH
		case CS, ES, FR, NL, JA -> Language.EN;
		case DE, EN -> language;
		};

		var translationBundle = ResourceBundle.getBundle("io.openems.edge.core.appmanager.validator.translation",
				availableLanguage.getLocal());
		return TranslationUtil.getTranslation(translationBundle, key, params);
	}

}
