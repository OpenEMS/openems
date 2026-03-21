package io.openems.edge.app.enums;

import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;

public class SafetyParameterTranslatableEnum {

	public class Vrt {

		public enum CurrentDistributionMode implements TranslatableEnum {
			REACTIVE_POWER_PRIO("App.IntegratedSystem.safetyParameter.currentDistributionMode.ReactivePowerPrio"), //
			ACTIVE_POWER_PRIO("App.IntegratedSystem.safetyParameter.currentDistributionMode.ActivePowerPrio"), //
			CONSTANT_CURRENT("App.IntegratedSystem.safetyParameter.currentDistributionMode.ConstantCurrent");

			private final String translationKey;

			private CurrentDistributionMode(String translationKey) {
				this.translationKey = translationKey;
			}

			/**
			 * Gets the optionsFactory of this Enum.
			 *
			 * @return the {@link OptionsFactory}
			 */
			public static OptionsFactory optionsFactory() {
				return OptionsFactory.of(CurrentDistributionMode.class);
			}

			@Override
			public String getTranslation(Language language) {
				final var bundle = AbstractOpenemsApp.getTranslationBundle(language);
				return TranslationUtil.getTranslation(bundle, this.translationKey);
			}
		}

		public enum GeneralRecoveryMode implements TranslatableEnum {
			DISABLE("disable"), //
			GRADIENT_CONTROL("App.IntegratedSystem.safetyParameter.GeneralRecoveryMode.GradientControl"), //
			PT_1_BEHAVIOUR("App.IntegratedSystem.safetyParameter.GeneralRecoveryMode.PtBehaviour");

			private final String translationKey;

			private GeneralRecoveryMode(String translationKey) {
				this.translationKey = translationKey;
			}

			/**
			 * Gets the optionsFactory of this Enum.
			 *
			 * @return the {@link OptionsFactory}
			 */
			public static OptionsFactory optionsFactory() {
				return OptionsFactory.of(GeneralRecoveryMode.class);
			}

			@Override
			public String getTranslation(Language language) {
				final var bundle = AbstractOpenemsApp.getTranslationBundle(language);
				return TranslationUtil.getTranslation(bundle, this.translationKey);
			}

		}
	}

	public class Rpm {

		public enum Mode implements TranslatableEnum {
			BASIC("App.IntegratedSystem.safetyParameter.rpm.mode.basic"),
			SLOPE("App.IntegratedSystem.safetyParameter.rpm.mode.slope");

			private final String translationKey;

			private Mode(String translationKey) {
				this.translationKey = translationKey;
			}

			/**
			 * Gets the optionsFactory of this Enum.
			 *
			 * @return the {@link OptionsFactory}
			 */
			public static OptionsFactory optionsFactory() {
				return OptionsFactory.of(Rpm.Mode.class);
			}

			@Override
			public String getTranslation(Language language) {
				final var bundle = AbstractOpenemsApp.getTranslationBundle(language);
				return TranslationUtil.getTranslation(bundle, this.translationKey);
			}
		}
	}
}
