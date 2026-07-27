import { TranslateService } from "@ngx-translate/core";
import { HeatStatus } from "../shared/shared";

export namespace HeatConverter {
    /**
     * Converts Power2Heat-State
     *
     * @param translate The current language to be translated to
     * @returns Converted value
     */
    export const CONVERT_POWER_2_HEAT_STATE = (translate: TranslateService) => {
        return (value: any): string => {
            switch (value) {
                case HeatStatus.EXCESS:
                    return translate.instant("EDGE.INDEX.WIDGETS.HEAT.HEATING");
                case HeatStatus.CONTROL_NOT_ALLOWED:
                    return translate.instant("EDGE.INDEX.WIDGETS.HEAT.CONTROL_NOT_ALLOWED");
                case HeatStatus.TEMPERATURE_REACHED:
                    return translate.instant("EDGE.INDEX.WIDGETS.HEAT.TARGET_TEMPERATURE_REACHED");
                case HeatStatus.ERROR:
                    return translate.instant("EDGE.INDEX.WIDGETS.HEAT.ERROR");
                case HeatStatus.UNDEFINED: //case the same as default
                case HeatStatus.STANDBY: //case the same as default
                case HeatStatus.NO_CONTROL_SIGNAL: //case the same as default
                default:
                    return translate.instant("EDGE.INDEX.WIDGETS.HEAT.NO_HEATING");
            }
        };
    };
}
