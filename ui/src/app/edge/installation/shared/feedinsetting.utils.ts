import { TranslateService } from "@ngx-translate/core";
import { FeedInSetting } from "./enums";
import { FEED_IN_POWER_FACTOR_OPTIONS } from "./options";

export namespace FeedInSettingUtils {
    export function getFeedInSettingLabel(feedInSetting: FeedInSetting, translate: TranslateService) {
        switch (feedInSetting) {
            case FeedInSetting.QuEnableCurve:
                return translate.instant("INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.QU_ENABLED_CURVE");
            case FeedInSetting.PuEnableCurve:
                return translate.instant("INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.PU_ENABLED_CURVE");
            case FeedInSetting.FixedPowerFactor:
                return translate.instant("INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.FIXED_POWER_FACTOR");
            default:
                return feedInSetting;
        }
    }

    export const getFeedInPowerFactorLabel = (
        value: FeedInSetting,
        translate: TranslateService
    ): string => {
        const options = FEED_IN_POWER_FACTOR_OPTIONS(translate);
        const found = options.find(opt => opt.value === value);
        return found ? found.label : value; // fallback to enum string if not found
    };
}
