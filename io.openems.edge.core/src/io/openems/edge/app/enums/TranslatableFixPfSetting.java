package io.openems.edge.app.enums;

import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;

public enum TranslatableFixPfSetting implements TranslatableEnum {

	LAGGING_0_99(99, Type.LAGGING), //
	LAGGING_0_98(98, Type.LAGGING), //
	LAGGING_0_97(97, Type.LAGGING), //
	LAGGING_0_96(96, Type.LAGGING), //
	LAGGING_0_95(95, Type.LAGGING), //
	LAGGING_0_94(94, Type.LAGGING), //
	LAGGING_0_93(93, Type.LAGGING), //
	LAGGING_0_92(92, Type.LAGGING), //
	LAGGING_0_91(91, Type.LAGGING), //
	LAGGING_0_90(90, Type.LAGGING), //
	LAGGING_0_89(89, Type.LAGGING), //
	LAGGING_0_88(88, Type.LAGGING), //
	LAGGING_0_87(87, Type.LAGGING), //
	LAGGING_0_86(86, Type.LAGGING), //
	LAGGING_0_85(85, Type.LAGGING), //
	LAGGING_0_84(84, Type.LAGGING), //
	LAGGING_0_83(83, Type.LAGGING), //
	LAGGING_0_82(82, Type.LAGGING), //
	LAGGING_0_81(81, Type.LAGGING), //
	LAGGING_0_80(80, Type.LAGGING), //
	LEADING_0_80(80, Type.LEADING), //
	LEADING_0_81(81, Type.LEADING), //
	LEADING_0_82(82, Type.LEADING), //
	LEADING_0_83(83, Type.LEADING), //
	LEADING_0_84(84, Type.LEADING), //
	LEADING_0_85(85, Type.LEADING), //
	LEADING_0_86(86, Type.LEADING), //
	LEADING_0_87(87, Type.LEADING), //
	LEADING_0_88(88, Type.LEADING), //
	LEADING_0_89(89, Type.LEADING), //
	LEADING_0_90(90, Type.LEADING), //
	LEADING_0_91(91, Type.LEADING), //
	LEADING_0_92(92, Type.LEADING), //
	LEADING_0_93(93, Type.LEADING), //
	LEADING_0_94(94, Type.LEADING), //
	LEADING_0_95(95, Type.LEADING), //
	LEADING_0_96(96, Type.LEADING), //
	LEADING_0_97(97, Type.LEADING), //
	LEADING_0_98(98, Type.LEADING), //
	LEADING_0_99(99, Type.LEADING), //
	LEADING_1_OR_NONE(0, Type.NONE);

	private final int number;
	private final Type type;

	TranslatableFixPfSetting(int number, Type type) {
		this.number = number;
		this.type = type;
	}

	/**
	 * Gets the optionsFactory of this Enum.
	 * 
	 * @return the {@link OptionsFactory}
	 */
	public static OptionsFactory optionsFactory() {
		return OptionsFactory.of(TranslatableFixPfSetting.class);
	}

	@Override
	public String getTranslation(Language language) {
		final var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		var translationKey = switch (this.type) {
		case Type.LAGGING -> "App.IntegratedSystem.feedInSettings.lagging";
		case Type.LEADING -> "App.IntegratedSystem.feedInSettings.leading";
		case Type.NONE -> "App.IntegratedSystem.feedInSettings.cosPhiFixValue";
		};
		if (this.type == Type.NONE) {
			return TranslationUtil.getTranslation(bundle, translationKey);
		}
		return TranslationUtil.getTranslation(bundle, translationKey, this.number / 100.0);
	}

	private enum Type {
		LAGGING, LEADING, NONE
	}
}
