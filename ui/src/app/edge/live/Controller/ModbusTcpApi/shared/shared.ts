import { TranslateService } from "@ngx-translate/core";
import { NavigationConstants, NavigationTree, } from "src/app/shared/components/navigation/shared";
import { Converter } from "src/app/shared/components/shared/converter";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { OverrideStatus } from "src/app/shared/type/general";

export namespace SharedControllerModbusTcpApiReadWrite {
    export function getNavigationTree(
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): ConstructorParameters<typeof NavigationTree> {
        return new NavigationTree(
            "modbus-tcp-api/" + component.id,
            { baseString: "controller/modbus-tcp-api/" + component.id },
            { name: "swap-vertical-outline", color: "normal" },
            component.alias,
            "label",
            [
                NavigationConstants.CommonNodes.SETTINGS(translate),
                NavigationConstants.CommonNodes.HISTORY(translate),
            ],
            null,
        ).toConstructorParams();
    }

    export const TO_OVERRIDE_STATUS_LABEL = (
        translate: TranslateService,
    ): Converter => {
        return (raw): string => {
            return Converter.IF_NUMBER(raw, (value) => {
                switch (value) {
                    case OverrideStatus.ACTIVE:
                        return translate.instant(
                            "MODBUS_TCP_API_READ_WRITE.OVERRIDING",
                        );
                    case OverrideStatus.ERROR:
                        return translate.instant("GENERAL.FAULT");
                    default:
                        return translate.instant(
                            "MODBUS_TCP_API_READ_WRITE.NOT_OVERRIDING",
                        );
                }
            });
        };
    };

    export const TO_TRANSLATED_CHANNEL = (translate: TranslateService) => {
        const keys: Record<string, string> = {
            SetActivePowerEquals: "SET_ACTIVE_POWER_EQUALS",
            SetActivePowerGreaterOrEquals: "SET_ACTIVE_POWER_GREATER_OR_EQUALS",
            SetActivePowerLessOrEquals: "SET_ACTIVE_POWER_LESS_OR_EQUALS",
        };

        return (raw: ChannelAddress): string | null => {
            if (!raw?.channelId.includes("Ess0SetActive")) {
                return null;
            }

            const key = keys[raw.channelId.replace("Ess0", "")];
            return key
                ? translate.instant(`MODBUS_TCP_API_READ_WRITE.${key}`)
                : null;
        };
    };
}
