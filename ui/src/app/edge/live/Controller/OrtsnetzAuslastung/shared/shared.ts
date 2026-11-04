import { TranslateService } from "@ngx-translate/core";

export namespace SharedControllerOrtsnetzAuslastung {
    const I18N = "EDGE.INDEX.WIDGETS.ORTSNETZ_AUSLASTUNG.";

    /** See Types.GridStatus of io.openems.edge.controller.ortsnetzauslastung. */
    export enum GridStatus {
        UNDEFINED = -1,
        GREEN = 0,
        YELLOW = 1,
        RED = 2,
    }

    /** See Types.StorageRecommendation of io.openems.edge.controller.ortsnetzauslastung. */
    export enum StorageRecommendation {
        UNDEFINED = -1,
        NONE = 0,
        CHARGE = 1,
        DISCHARGE = 2,
    }

    /** State-Channels that are set if no measurement is sent or accepted, with the translation key of their text. */
    export const PROBLEM_CHANNELS: { channelId: string; key: string }[] = [
        { channelId: "ThirdPartyUsageNotAccepted", key: I18N + "THIRD_PARTY_USAGE_NOT_ACCEPTED" },
        { channelId: "NoCoordinates", key: I18N + "NO_COORDINATES" },
        { channelId: "NoGridMeter", key: I18N + "NO_GRID_METER" },
        { channelId: "NoVoltage", key: I18N + "NO_VOLTAGE" },
        { channelId: "LocationBlocked", key: I18N + "LOCATION_BLOCKED" },
        { channelId: "RequestRejected", key: I18N + "REQUEST_REJECTED" },
        { channelId: "UnableToSend", key: I18N + "UNABLE_TO_SEND" },
    ];

    export const CONVERT_GRID_STATUS = (translate: TranslateService) => {
        return (value: number | null | undefined): string => {
            switch (value) {
                case GridStatus.GREEN:
                    return translate.instant(I18N + "GREEN");
                case GridStatus.YELLOW:
                    return translate.instant(I18N + "YELLOW");
                case GridStatus.RED:
                    return translate.instant(I18N + "RED");
                default:
                    return translate.instant(I18N + "NO_DATA");
            }
        };
    };

    export const CONVERT_STORAGE_RECOMMENDATION = (translate: TranslateService) => {
        return (value: number | null | undefined): string => {
            switch (value) {
                case StorageRecommendation.NONE:
                    return translate.instant(I18N + "RECOMMENDATION_NONE");
                case StorageRecommendation.CHARGE:
                    return translate.instant(I18N + "RECOMMENDATION_CHARGE");
                case StorageRecommendation.DISCHARGE:
                    return translate.instant(I18N + "RECOMMENDATION_DISCHARGE");
                default:
                    return translate.instant(I18N + "NO_DATA");
            }
        };
    };

    export const CONVERT_YES_NO = (translate: TranslateService) => {
        return (value: number | boolean | null | undefined): string => {
            if (value === null || value === undefined) {
                return translate.instant(I18N + "NO_DATA");
            }
            return translate.instant(value ? "GENERAL.YES" : "GENERAL.NO");
        };
    };

    /**
     * Gets the color of the widget icon for the overall grid status.
     *
     * @param value the value of the Channel GridStatus
     * @returns the name of the color
     */
    export function getStatusColor(value: number | null | undefined): string {
        switch (value) {
            case GridStatus.GREEN:
                return "success";
            case GridStatus.YELLOW:
                return "warning";
            case GridStatus.RED:
                return "danger";
            default:
                return "normal";
        }
    }

    /**
     * Gets the translation keys of all active problems.
     *
     * @param componentId the Component-ID
     * @param data the current Channel values
     * @returns the translation keys
     */
    export function getProblemKeys(componentId: string, data: { [channelAddress: string]: any }): string[] {
        return PROBLEM_CHANNELS //
            .filter((problem) => !!data[componentId + "/" + problem.channelId]) //
            .map((problem) => problem.key);
    }
}
