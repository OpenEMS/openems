import { TranslateService } from "@ngx-translate/core";
import { NavigationTree } from "src/app/shared/components/navigation/shared";
import { Name } from "src/app/shared/components/shared/name";
import { OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";

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
     * Gets the addresses of all problem Channels.
     *
     * @param componentId the Component-ID
     * @returns the Channel addresses
     */
    export function getProblemChannelAddresses(componentId: string): ChannelAddress[] {
        return PROBLEM_CHANNELS.map((problem) => new ChannelAddress(componentId, problem.channelId));
    }

    /**
     * Gets the {@link NavigationTree} for the new design. It is a child of the grid node.
     *
     * @param translate the translate service
     * @param component the Component
     * @returns the constructor parameters of the {@link NavigationTree}
     */
    export function getNavigationTree(
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): ConstructorParameters<typeof NavigationTree> {
        return new NavigationTree(
            component.id,
            { baseString: "controller/ortsnetz-auslastung/" + component.id },
            { name: "pulse-outline", color: "normal" },
            Name.METER_ALIAS_OR_ID(component),
            "label",
            [],
            null,
        ).toConstructorParams();
    }

    /**
     * Gets the view for the new design.
     *
     * @param translate the translate service
     * @param component the Component
     * @returns the {@link OeFormlyView}
     */
    export function getFormlyView(translate: TranslateService, component: EdgeConfig.Component): OeFormlyView {
        const id = component.id;
        const convertGridStatus = CONVERT_GRID_STATUS(translate);
        const problemChannels = getProblemChannelAddresses(id);

        return {
            title: Name.METER_ALIAS_OR_ID(component),
            component: component,
            lines: [
                {
                    type: "channel-line",
                    name: translate.instant(I18N + "GRID_STATUS"),
                    channel: id + "/GridStatus",
                    converter: convertGridStatus,
                },
                ...["L1", "L2", "L3"].map((phase) => ({
                    type: "channel-line" as const,
                    name: translate.instant("GENERAL.PHASE") + " " + phase,
                    channel: id + "/GridStatus" + phase,
                    converter: convertGridStatus,
                })),
                { type: "horizontal-line" },
                {
                    type: "channel-line",
                    name: translate.instant(I18N + "STORAGE_RECOMMENDATION"),
                    channel: id + "/StorageRecommendation",
                    converter: CONVERT_STORAGE_RECOMMENDATION(translate),
                },
                {
                    type: "channel-line",
                    name: translate.instant(I18N + "REDISPATCH_ACTIVE"),
                    channel: id + "/RedispatchActive",
                    converter: CONVERT_YES_NO(translate),
                },
                {
                    type: "value-from-channels-line",
                    name: translate.instant(I18N + "TRANSMISSION"),
                    channelsToSubscribe: problemChannels,
                    filter: (currentData) => getProblemKeys(id, currentData.allComponents).length > 0,
                    value: () => translate.instant(I18N + "TRANSMISSION_PROBLEM"),
                },
                ...PROBLEM_CHANNELS.map((problem) => ({
                    type: "value-from-channels-line" as const,
                    channelsToSubscribe: problemChannels,
                    filter: (currentData: { allComponents: { [channelAddress: string]: any } }) =>
                        getProblemKeys(id, currentData.allComponents).includes(problem.key),
                    value: () => translate.instant(problem.key),
                    singleLine: true,
                })),
            ],
        };
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
