import { TranslateService } from "@ngx-translate/core";
import { NavigationConstants, NavigationTree } from "src/app/shared/components/navigation/shared";
import { Converter } from "src/app/shared/components/shared/converter";
import { EdgeConfig } from "src/app/shared/shared";
import { ChannelAddress } from "src/app/shared/type/channeladdress";
import { OverrideStatus } from "src/app/shared/type/general";

export namespace SharedControllerModbusTcpApiReadWrite {
    export type ChannelSuffix =
        | "SetActivePowerEquals"
        | "SetActivePowerGreaterOrEquals"
        | "SetActivePowerLessOrEquals"
        | "SetReactivePowerEquals"
        | "SetReactivePowerGreaterOrEquals"
        | "SetReactivePowerLessOrEquals";

    /** IDs supported for TCP/Modbus write channels. */
    export type ChannelPrefix = `Ess${number}`;
    export type ChannelId = `${ChannelPrefix}${ChannelSuffix}`;

    /**
     * Represents a TCP/Modbus write channel. Channels of the same type are currently not differentiated by color or
     * label. For example, Ess0SetActivePowerEquals and Ess1SetActivePowerEquals are displayed with the same color and
     * label.
     */
    export class ModbusTcpApiChannel extends ChannelAddress {
        private channel: {
            prefix: ChannelPrefix;
            suffix: ChannelSuffix;
        };

        constructor(
            public override readonly componentId: string,
            public override readonly channelId: string,
        ) {
            super(componentId, channelId);
            const match = channelId.match(
                /^(Ess\d+)(SetActivePowerEquals|SetActivePowerGreaterOrEquals|SetActivePowerLessOrEquals|SetReactivePowerEquals|SetReactivePowerGreaterOrEquals|SetReactivePowerLessOrEquals)$/,
            );

            if (!match) {
                throw new Error(`Invalid channel ID: ${channelId}`);
            }

            this.channel = {
                prefix: match[1] as ChannelPrefix,
                suffix: match[2] as ChannelSuffix,
            };
        }

        /** Returns the channel name without the ESS prefix. */
        get channelSuffix(): ChannelSuffix {
            return this.channel.suffix;
        }

        /** Returns the prefix. */
        get channelPrefix(): ChannelPrefix {
            return this.channel.prefix;
        }

        get color(): string {
            return CHANNEL_COLORS[this.channelSuffix];
        }

        translatedName(translate: TranslateService): string {
            //The switch is intentional: i18n Ally requires string literals inside translate.instant() to display the translations inline.
            switch (this.channelSuffix) {
                case "SetActivePowerEquals":
                    return translate.instant("MODBUS_TCP_API_READ_WRITE.SET_ACTIVE_POWER_EQUALS");

                case "SetActivePowerGreaterOrEquals":
                    return translate.instant("MODBUS_TCP_API_READ_WRITE.SET_ACTIVE_POWER_GREATER_OR_EQUALS");

                case "SetActivePowerLessOrEquals":
                    return translate.instant("MODBUS_TCP_API_READ_WRITE.SET_ACTIVE_POWER_LESS_OR_EQUALS");

                case "SetReactivePowerEquals":
                    return translate.instant("MODBUS_TCP_API_READ_WRITE.SET_REACTIVE_POWER_EQUALS");

                case "SetReactivePowerGreaterOrEquals":
                    return translate.instant("MODBUS_TCP_API_READ_WRITE.SET_REACTIVE_POWER_GREATER_OR_EQUALS");

                case "SetReactivePowerLessOrEquals":
                    return translate.instant("MODBUS_TCP_API_READ_WRITE.SET_REACTIVE_POWER_LESS_OR_EQUALS");
            }
        }
    }

    /** Maps each {@link ChannelSuffix} to the RGB color used to display it in a chart. */
    export const CHANNEL_COLORS: Record<ChannelSuffix, string> = {
        SetActivePowerLessOrEquals: "rgb(206, 134, 52)",
        SetActivePowerEquals: "rgb(211, 46, 46)",
        SetActivePowerGreaterOrEquals: "rgb(75, 4, 4)",
        SetReactivePowerLessOrEquals: "rgb(12, 133, 255)",
        SetReactivePowerEquals: "rgb(2, 0, 117)",
        SetReactivePowerGreaterOrEquals: "rgb(61, 20, 128)",
    };

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
            [NavigationConstants.CommonNodes.SETTINGS(translate), NavigationConstants.CommonNodes.HISTORY(translate)],
            null,
        ).toConstructorParams();
    }

    export const TO_OVERRIDE_STATUS_LABEL = (translate: TranslateService): Converter => {
        return (raw): string => {
            return Converter.IF_NUMBER(raw, (value) => {
                switch (value) {
                    case OverrideStatus.ACTIVE:
                        return translate.instant("MODBUS_TCP_API_READ_WRITE.OVERRIDING");
                    case OverrideStatus.ERROR:
                        return translate.instant("GENERAL.FAULT");
                    default:
                        return translate.instant("MODBUS_TCP_API_READ_WRITE.NOT_OVERRIDING");
                }
            });
        };
    };
}
