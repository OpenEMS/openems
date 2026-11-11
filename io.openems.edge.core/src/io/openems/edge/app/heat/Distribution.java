package io.openems.edge.app.heat;

import io.openems.common.session.Language;
import io.openems.edge.app.enums.TranslatableEnum;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;

/**
 * The switching order of the Levels of both heating elements, used by the
 * {@link io.openems.edge.app.heat.HeatingElement} App.
 *
 * <p>
 * The names of this enum have to match the names of
 * {@code io.openems.edge.controller.io.heatingelement.enums.Distribution} of
 * the controller bundle, since the selected value is written as a plain String
 * into the components configuration.
 */
public enum Distribution implements TranslatableEnum {
	/**
	 * Heating Element A is fully activated before Heating Element B is activated:
	 * A-L1 -&gt; A-L2 -&gt; A-L3 -&gt; B-L1 -&gt; B-L2 -&gt; B-L3.
	 */
	SEQUENTIAL("App.Heat.HeatingElement.distribution.sequential"), //
	/**
	 * Heating Elements A and B are activated alternately: A-L1 -&gt; B-L1 -&gt;
	 * A-L2 -&gt; B-L2 -&gt; A-L3 -&gt; B-L3.
	 */
	PARALLEL("App.Heat.HeatingElement.distribution.parallel"), //
	;

	private final String translationKey;

	private Distribution(String translationKey) {
		this.translationKey = translationKey;
	}

	@Override
	public String getTranslation(Language language) {
		final var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return TranslationUtil.getTranslation(bundle, this.translationKey);
	}

}
