import { FormControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { GroupedNavigationTreeUtility, NavigationConstants, NavigationTree, } from "src/app/shared/components/navigation/shared";
import { Converter } from "src/app/shared/components/shared/converter";
import { Filter } from "src/app/shared/components/shared/filter";
import { Formatter } from "src/app/shared/components/shared/formatter";
import { OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { CurrentDataUtils } from "src/app/shared/type/currentdata";
import { Mode } from "src/app/shared/type/general";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { StringUtils } from "src/app/shared/utils/string/string.utils";

export namespace SharedIoChannelSingleThreshold {
    type KnownInputChannelAddress = "_sum/EssSoc" | "_sum/GridActivePower" | "_sum/ProductionActivePower";

    const isKnownInputChannelAddress = (inputChannelAddress: string): inputChannelAddress is KnownInputChannelAddress =>
        inputChannelAddress === "_sum/EssSoc" ||
        inputChannelAddress === "_sum/GridActivePower" ||
        inputChannelAddress === "_sum/ProductionActivePower";

    export type InputMode = "SOC" | "GRIDSELL" | "GRIDBUY" | "PRODUCTION" | "OTHER";

    export const getFormlyView = async (
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
        websocket: Websocket,
    ): Promise<
        OeFormlyView<{
            mode: Mode;
            inputChannelAddressToggleValue: "SOC" | "GRIDSELL" | "GRIDBUY" | "PRODUCTION";
        }>
    > => {
        return {
            title: component.alias,
            helpKey: "REDIRECT.CONTROLLER_IO_CHANNEL_SINGLE_THRESHOLD",
            lines: [
                ...getFormlySharedLines(translate, component),
                ...(await getFormlyAutomaticView(translate, edge, websocket)),
            ],
            component: component,
            edge: edge,
        };
    };

    const getFormlyAutomaticView = async (
        translate: TranslateService,
        edge: Edge,
        websocket: Websocket,
    ): Promise<
        OeFormlyView<{
            mode: Mode;
            inputChannelAddressToggleValue: "SOC" | "GRIDSELL" | "GRIDBUY" | "PRODUCTION";
        }>["lines"]
    > => {
        const lines: OeFormlyView<{
            mode: Mode;
            inputChannelAddressToggleValue: "SOC" | "GRIDSELL" | "GRIDBUY" | "PRODUCTION";
        }>["lines"] = [];

        const gridActivePowerChannelAddress = new ChannelAddress("_sum", "GridActivePower");
        const productionActivePowerChannelAddress = new ChannelAddress("_sum", "ProductionActivePower");
        const getUnit = await createUnitResolver(edge, websocket, [
            gridActivePowerChannelAddress,
            productionActivePowerChannelAddress,
        ]);

        lines.push(
            {
                type: "select-line",
                controlName: "inputChannelAddressToggleValue",
                name: translate.instant("EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.DEPENDEND_ON"),
                options: [
                    { name: translate.instant("GENERAL.SOC"), value: "SOC" },
                    {
                        name: translate.instant("GENERAL.PRODUCTION"),
                        value: "PRODUCTION",
                    },
                    {
                        name: translate.instant("GENERAL.GRID_SELL"),
                        value: "GRIDSELL",
                    },
                    {
                        name: translate.instant("GENERAL.GRID_BUY"),
                        value: "GRIDBUY",
                    },
                ],
                hide: HIDE_ON_MODE_NOT_AUTOMATIC,
            },

            {
                type: "input-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCHED_LOAD_POWER"),
                controlName: "switchedLoadPower",
                properties: {
                    unit: "W",
                },
                hide: HIDE_ON_MODE_NOT_AUTOMATIC_OR_SOC_OR_PRODUCTION,
            },
            {
                type: "horizontal-line",
                hide: HIDE_ON_MODE_NOT_AUTOMATIC_OR_SOC_OR_PRODUCTION,
            },
            // SOC
            {
                type: "channel-line",
                name: translate.instant("GENERAL.CURRENT_VALUE"),
                channel: new ChannelAddress("_sum", "EssSoc").toString(),
                converter: (value) => value + " %",
                hide: HIDE_ON_NOT_AUTOMATIC_OR_NOT_SOC,
            },
            // GRIDSELL -
            {
                type: "channel-line",
                name: translate.instant("GENERAL.CURRENT_VALUE"),
                channel: new ChannelAddress("_sum", "GridActivePower").toString(),
                converter: createGridPowerConverter(getUnit, gridActivePowerChannelAddress, -1),
                hide: HIDE_ON_NOT_AUTOMATIC_OR_NOT_GRIDSELL,
            },
            // GRIDBUY
            {
                type: "channel-line",
                name: translate.instant("GENERAL.CURRENT_VALUE"),
                channel: new ChannelAddress("_sum", "GridActivePower").toString(),
                converter: createGridPowerConverter(getUnit, gridActivePowerChannelAddress, 1),
                hide: HIDE_ON_NOT_AUTOMATIC_OR_NOT_GRIDBUY,
            },
            // PRODUCTION
            {
                type: "channel-line",
                name: translate.instant("GENERAL.CURRENT_VALUE"),
                channel: new ChannelAddress("_sum", "ProductionActivePower").toString(),
                converter: (value) => {
                    const unit = getUnit(productionActivePowerChannelAddress);

                    return SharedIoChannelSingleThreshold.createCurrentValueLabel(value, unit) ?? String(value);
                },
                hide: HIDE_ON_MODE_NOT_AUTOMATIC_OR_NOT_PRODUCTION,
            },
            {
                type: "value-from-form-control-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.THRESHOLD"),
                controlName: "threshold",
                converter: Converter.TO_PERCENT,
                hide: HIDE_ON_NOT_AUTOMATIC_OR_NOT_SOC,
            },
            {
                type: "input-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.THRESHOLD"),
                controlName: "threshold",
                properties: {
                    unit: "W",
                },
                hide: HIDE_ON_NOT_AUTOMATIC_OR_IS_SOC,
            },
            {
                type: "range-button-from-form-control-line",
                controlName: "threshold",
                properties: {
                    tickMin: 0,
                    tickMax: 100,
                    step: 1,
                    unit: "%",
                },
                hide: HIDE_ON_NOT_AUTOMATIC_OR_NOT_SOC,
            },
        );
        lines.push(
            {
                type: "horizontal-line",
                hide: HIDE_ON_MODE_NOT_AUTOMATIC,
            },
            //regular behaviour for invert if GridSell is not selected
            {
                type: "buttons-from-form-control-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.BEHAVIOUR"),
                controlName: "invert",
                buttons: [
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_ABOVE_THRESHOLD"),
                        value: false,
                        icon: {
                            color: "success",
                            name: "arrow-up-outline",
                            size: "medium",
                        },
                    },
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_BELOW_THRESHOLD"),
                        value: true,
                        icon: {
                            color: "danger",
                            name: "arrow-down-outline",
                            size: "medium",
                        },
                    },
                ],
                hide: HIDE_ON_NOT_AUTOMATIC_OR_IS_GRIDSELL,
            },
            // invert behaviour for invert if GridSell is selected
            {
                type: "buttons-from-form-control-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.BEHAVIOUR"),
                controlName: "invert",
                buttons: [
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_ABOVE_THRESHOLD"),
                        value: true,
                        icon: {
                            color: "success",
                            name: "arrow-up-outline",
                            size: "medium",
                        },
                    },
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_BELOW_THRESHOLD"),
                        value: false,
                        icon: {
                            color: "danger",
                            name: "arrow-down-outline",
                            size: "medium",
                        },
                    },
                ],
                hide: HIDE_ON_NOT_AUTOMATIC_OR_NOT_GRIDSELL,
            },
            {
                type: "horizontal-line",
                hide: HIDE_ON_MODE_NOT_AUTOMATIC,
            },
            {
                type: "input-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.MIN_SWITCHING_TIME"),
                controlName: "minimumSwitchingTime",
                properties: {
                    unit: "s",
                },
                hide: HIDE_ON_MODE_NOT_AUTOMATIC,
            },
        );

        return lines;
    };

    const getFormlySharedLines = (
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): OeFormlyView["lines"] => {
        const lines: OeFormlyView["lines"] = [];
        const outputChannelAddress = component.getPropertyFromComponent<string[]>("outputChannelAddress");

        if (outputChannelAddress != null && outputChannelAddress.length > 0) {
            lines.push({
                type: "channel-line",
                name: translate.instant("GENERAL.STATE"),
                channel: outputChannelAddress[0],
                converter: Converter.ON_OFF(translate),
            });
        }

        lines.push(
            {
                type: "horizontal-line",
            },
            {
                type: "buttons-from-form-control-line",
                name: translate.instant("GENERAL.MODE"),
                controlName: "mode",
                buttons: [
                    {
                        name: translate.instant("GENERAL.ON"),
                        value: "ON",
                        icon: {
                            color: "success",
                            name: "play-outline",
                            size: "medium",
                        },
                    },
                    {
                        name: translate.instant("GENERAL.AUTOMATIC"),
                        value: "AUTOMATIC",
                        icon: {
                            color: "danger",
                            name: "sunny-outline",
                            size: "medium",
                        },
                    },
                    {
                        name: translate.instant("GENERAL.OFF"),
                        value: "OFF",
                        icon: {
                            color: "success",
                            name: "power-outline",
                            size: "medium",
                        },
                    },
                ],
            },
            {
                type: "horizontal-line",
            },
        );

        return lines;
    };

    export const getFormlyHomeLines = (
        translate: TranslateService,
        component: EdgeConfig.Component,
        outputChannel: string[],
        inputChannel: string,
        getUnit: (channelAddress: ChannelAddress) => string | null,
    ): OeFormlyView["lines"] => {
        const lines: OeFormlyView["lines"] = [];

        if (outputChannel == null || inputChannel == null || outputChannel.length == 0) {
            return lines;
        }

        lines.push({
            type: "channel-line",
            name: translate.instant("GENERAL.STATE"),
            channel: outputChannel[0],
            converter: Converter.ON_OFF(translate),
        });

        lines.push({
            type: "channel-line",
            name: translate.instant("GENERAL.MODE"),
            channel: component.id + "/_PropertyMode",
            converter: Converter.CONTROLLER_PROPERTY_MODES(translate),
        });

        if (outputChannel != null) {
            lines.push(
                {
                    type: "channel-line",
                    name: translate.instant("EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.DEPENDEND_ON"),
                    channel: component.id + outputChannel,
                    converter: (_value) =>
                        Converter.IF_STRING(inputChannel, (channel) => {
                            const inputChannelAddress = ChannelAddress.fromString(channel);
                            return SharedIoChannelSingleThreshold.createDependenOnLabel(
                                inputChannelAddress,
                                translate,
                                component,
                            );
                        }),
                },
                {
                    type: "channel-line",
                    name: translate.instant("GENERAL.CURRENT_VALUE"),
                    channel: component.id + outputChannel,
                    converter: (value) => {
                        const unit = getUnit(ChannelAddress.fromString(inputChannel));

                        return SharedIoChannelSingleThreshold.createCurrentValueLabel(value, unit) ?? "";
                    },
                    filter: (value) => {
                        if (inputChannel == null) {
                            return false;
                        }
                        const isOtherInputAddress = StringUtils.isNotInArr(inputChannel.toString(), [
                            "_sum/EssSoc",
                            "_sum/GridActivePower",
                            "_sum/ProductionActivePower",
                        ]);

                        return Filter.NOT_NULL_OR_UNDEFINED(value) && isOtherInputAddress;
                    },
                },
            );
        }

        lines.push({
            type: "value-from-channels-line",
            name: {
                converter: (value: string | number | null): string =>
                    Converter.IF_NUMBER(value, (outputChannelValue) => {
                        const threshold = component.getPropertyFromComponent<number>("threshold");
                        const invert = component.getPropertyFromComponent<boolean>("invert");
                        const isThresholdPositive = threshold !== null && threshold > 0;

                        const label =
                            SharedIoChannelSingleThreshold.SwitchStateLabel.find(
                                (el) =>
                                    el.invert === invert &&
                                    el.outputChannelValue === outputChannelValue &&
                                    el.propertyThresholdPositive === isThresholdPositive,
                            )?.label ?? null;

                        return label == null ? "" : translate.instant(label);
                    }),
                channel: new ChannelAddress(component.id, outputChannel[0]),
            },
            channelsToSubscribe: [ChannelAddress.fromString(inputChannel)],
            value: (currentData: CurrentData) => {
                const unit = getUnit(ChannelAddress.fromString(inputChannel));
                const dependendOnValue = getDependendOnValue(component, currentData);

                return SharedIoChannelSingleThreshold.createCurrentValueLabel(dependendOnValue, unit) ?? "";
            },
            filter: (currentData: CurrentData) => {
                if (inputChannel == null) {
                    return false;
                }
                const unit = getUnit(ChannelAddress.fromString(inputChannel));
                const dependendOnValue = getDependendOnValue(component, currentData);

                const value = SharedIoChannelSingleThreshold.createCurrentValueLabel(dependendOnValue, unit);
                const isOtherInputAddress = StringUtils.isNotInArr(inputChannel.toString(), [
                    "_sum/EssSoc",
                    "_sum/GridActivePower",
                    "_sum/ProductionActivePower",
                ]);

                return Filter.NOT_NULL_OR_UNDEFINED(value) && isOtherInputAddress;
            },
        });

        return lines;
    };

    export function getChannelAddresses(
        service: Service,
        routeService: RouteService,
        component: EdgeConfig.Component | null = null,
    ): Promise<ChannelAddress[]> {
        const edge = service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const channelSingleThresholdComponent =
            component ?? config.getComponentSafely(routeService.getRouteParam("componentId"));

        AssertionUtils.assertIsDefined(channelSingleThresholdComponent);
        return Promise.resolve([
            new ChannelAddress(channelSingleThresholdComponent.id, "_PropertyMode"),
            new ChannelAddress(channelSingleThresholdComponent.id, "_PropertyInputChannelAddress"),
            new ChannelAddress(channelSingleThresholdComponent.id, "_PropertyThreshold"),
            new ChannelAddress(channelSingleThresholdComponent.id, "_PropertySwitchedLoadPower"),
            new ChannelAddress(channelSingleThresholdComponent.id, "_PropertyMinimumSwitchingTime"),
            new ChannelAddress(channelSingleThresholdComponent.id, "_PropertyInvert"),
        ]);
    }

    export function getFormGroup(): FormGroup {
        return new FormGroup({
            mode: new FormControl(null),
            inputChannelAddress: new FormControl(null),
            threshold: new FormControl(null),
            minimumSwitchingTime: new FormControl(null),
            inputChannelAddressToggleValue: new FormControl(null),
            invert: new FormControl(null),
            switchedLoadPower: new FormControl(null),
        });
    }

    export function getNavigationTree(
        translate: TranslateService,
        componentId: EdgeConfig.Component["id"],
        config: EdgeConfig,
    ): ConstructorParameters<typeof NavigationTree> | null {
        const component = config.getComponentSafely(componentId);
        if (component == null) {
            return null;
        }

        const label = component.alias?.trim() || component.id;
        return createComponentNavigationTree(
            componentId,
            label,
            "controller/io-channel-single-threshold/" + componentId,
            translate,
        ).toConstructorParams();
    }

    function createComponentNavigationTree(
        id: string,
        label: string,
        baseString: string,
        translate: TranslateService,
    ): NavigationTree {
        return new NavigationTree(
            id,
            { baseString },
            { name: "aperture-outline", color: "normal" },
            label,
            "label",
            [NavigationConstants.CommonNodes.HISTORY(translate), NavigationConstants.CommonNodes.SETTINGS(translate)],
            null,
        );
    }

    export function getGroupedNavigationTree(
        translate: TranslateService,
        componentIds: EdgeConfig.Component["id"][],
        config: EdgeConfig,
        factoryId: EdgeConfig.Factory["id"],
    ): NavigationTree | null {
        return GroupedNavigationTreeUtility.createGroupedNavigationTree(
            "channel-single-threshold-controllers",
            { name: "aperture-outline", color: "normal" },
            "MENU.GROUPS.CHANNEL_SINGLE_THRESHOLD",
            "controller/io-channel-single-threshold",
            translate,
            componentIds,
            config,
            factoryId,
            (componentId) =>
                GroupedNavigationTreeUtility.getNavigationTreeAsChild(
                    translate,
                    componentId,
                    config,
                    createComponentNavigationTree,
                ),
        );
    }

    /**
     * Creates the dependent on label from given input channel
     *
     * @param inputChannel The chosen input channel
     * @param currentData The current data
     * @returns A label
     */
    export function createDependenOnLabel(
        inputChannel: ChannelAddress,
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): string {
        const inputChannelAddress = inputChannel.toString() as KnownInputChannelAddress;

        switch (inputChannelAddress) {
            case "_sum/EssSoc":
                return translate.instant("GENERAL.SOC");
            case "_sum/GridActivePower": {
                const propertyThreshold = component.getPropertyFromComponent<number>("threshold");
                if (propertyThreshold != null && propertyThreshold < 0) {
                    return translate.instant("GENERAL.GRID_SELL");
                }
                return translate.instant("GENERAL.GRID_BUY");
            }
            case "_sum/ProductionActivePower":
                return translate.instant("GENERAL.PRODUCTION");
            default:
                return translate.instant("EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.OTHER") + " (" + inputChannel + ")";
        }
    }

    /**
     * Gets the current value label in the form of e.g. "1000 W"
     *
     * @param dependendOnValue The value of the channel this controller dependends on
     * @param unitOfInputChannel The unit of the channel this controller dependends on
     * @returns The {@link dependendOnValue} and the {@link unitOfInputChannel} if defined, else null
     */
    export function createCurrentValueLabel(
        dependendOnValue: string | number | null,
        unitOfInputChannel: string | null,
    ): string | null {
        if (dependendOnValue == null || unitOfInputChannel == null) {
            return null;
        }
        return Formatter.formatSafelyWithSuffix(dependendOnValue, "1.0-0", unitOfInputChannel);
    }

    /**
     * Gets the switch state label
     *
     * @param invert The invert value
     * @param outputChannelValue The outputchannel value
     * @param threshold The threshold
     * @param translate The translate service
     * @returns A the switch state label
     */
    export function createSwitchStateLabel(
        invert: boolean,
        outputChannelValue: number | null,
        threshold: number | null,
        translate: TranslateService,
    ) {
        const isThresholdPositive = threshold !== null && threshold > 0;
        const label =
            SwitchStateLabel.find(
                (el) =>
                    el.invert === invert &&
                    el.outputChannelValue === outputChannelValue &&
                    el.propertyThresholdPositive === isThresholdPositive,
            )?.label ?? null;

        return label == null ? null : translate.instant(label);
    }

    export function convertToChannelAddress(inputMode: InputMode): string | null {
        switch (inputMode) {
            case "SOC":
                return "_sum/EssSoc";
            case "GRIDBUY":
                return "_sum/GridActivePower";
            case "GRIDSELL":
                return "_sum/GridActivePower";
            case "PRODUCTION":
                return "_sum/ProductionActivePower";
            default:
                return null;
        }
    }

    export function getInputMode(
        component: EdgeConfig.Component,
        inputChannelAddress: string,
        currentData: CurrentData,
    ): InputMode {
        const threshold = currentData.allComponents[component.id + "/_PropertyThreshold"];

        if (!isKnownInputChannelAddress(inputChannelAddress)) {
            return "OTHER";
        }

        switch (inputChannelAddress) {
            case "_sum/GridActivePower":
                if (threshold < 0) {
                    return "GRIDSELL";
                }
                if (threshold > 0) {
                    return "GRIDBUY";
                }
                return "OTHER";

            case "_sum/ProductionActivePower":
                return "PRODUCTION";

            case "_sum/EssSoc":
                return "SOC";

            default:
                return "OTHER";
        }
    }

    export function getInputModeFromChannel(
        component: EdgeConfig.Component,
        currentData: CurrentData,
        channelAddress: string,
    ) {
        const inputChannelAddress = currentData.allComponents[channelAddress];
        return getInputMode(component, inputChannelAddress, currentData);
    }

    export enum RelayState {
        OFF = 0,
        ON = 1,
    }

    export const SwitchStateLabel = [
        {
            propertyThresholdPositive: true,
            invert: true,
            outputChannelValue: RelayState.OFF,
            label: "EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_BELOW",
        },
        {
            propertyThresholdPositive: true,
            invert: true,
            outputChannelValue: RelayState.ON,
            label: "EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_BELOW",
        },
        {
            propertyThresholdPositive: true,
            invert: false,
            outputChannelValue: RelayState.OFF,
            label: "EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_ABOVE",
        },
        {
            propertyThresholdPositive: true,
            invert: false,
            outputChannelValue: RelayState.ON,
            label: "EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_ABOVE",
        },
        {
            propertyThresholdPositive: false,
            invert: true,
            outputChannelValue: RelayState.OFF,
            label: "EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_ABOVE",
        },
        {
            propertyThresholdPositive: false,
            invert: true,
            outputChannelValue: RelayState.ON,
            label: "EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_ABOVE",
        },
        {
            propertyThresholdPositive: false,
            invert: false,
            outputChannelValue: RelayState.OFF,
            label: "EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_BELOW",
        },
        {
            propertyThresholdPositive: false,
            invert: false,
            outputChannelValue: RelayState.ON,
            label: "EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.SWITCH_ON_BELOW",
        },
    ] as const;

    export const createUnitResolver = async (
        edge: Edge,
        websocket: Websocket,
        channelAddresses: ChannelAddress[] = [],
    ) => {
        const units = new Map<string, string | null>();
        const pendingRequests = new Map<string, Promise<string | null>>();

        await Promise.all(
            channelAddresses.map(async (channelAddress) => {
                const key = channelAddress.toString();
                if (units.has(key)) {
                    return;
                }

                const unit = (await edge.getChannel(websocket, channelAddress))?.unit ?? null;
                units.set(key, unit);
            }),
        );

        return (channelAddress: ChannelAddress): string | null => {
            const key = channelAddress.toString();

            if (units.has(key)) {
                return units.get(key) ?? null;
            }

            if (pendingRequests.has(key)) {
                return null;
            }

            const request = edge.getChannel(websocket, channelAddress).then((result) => {
                const unit = result?.unit ?? null;
                units.set(key, unit);
                pendingRequests.delete(key);
                return unit;
            });

            pendingRequests.set(key, request);
            return null;
        };
    };

    const createGridPowerConverter = (
        getUnit: (channelAddress: ChannelAddress) => string | null,
        channelAddress: ChannelAddress,
        direction: 1 | -1,
    ) => {
        return (raw: number | string | null) =>
            Converter.IF_NUMBER(raw, (value) => {
                const result = Math.max(0, direction * value);
                const unit = getUnit(channelAddress);

                return unit == null ? result.toString() : result.toString() + " " + unit;
            });
    };

    function getDependendOnValue(component: EdgeConfig.Component, currentData: CurrentData): string | null {
        const inputChannel = component.getPropertyFromComponent<KnownInputChannelAddress>("inputChannelAddress");
        if (inputChannel == null) {
            return null;
        }
        const inputChannelAddress = ChannelAddress.fromString(inputChannel);
        return CurrentDataUtils.getChannel<string>(inputChannelAddress, currentData.allComponents);
    }

    const HIDE_ON_MODE_NOT_AUTOMATIC = (el: { mode: Mode }) => el.mode !== Mode.AUTOMATIC;

    const HIDE_ON_MODE_NOT_AUTOMATIC_OR_SOC_OR_PRODUCTION = (el: {
        mode: Mode;
        inputChannelAddressToggleValue: InputMode;
    }) =>
        el.mode !== Mode.AUTOMATIC ||
        el.inputChannelAddressToggleValue === "SOC" ||
        el.inputChannelAddressToggleValue === "PRODUCTION";

    const HIDE_ON_NOT_AUTOMATIC_OR_IS_SOC = (el: { mode: Mode; inputChannelAddressToggleValue: InputMode }) =>
        el.mode !== Mode.AUTOMATIC || el.inputChannelAddressToggleValue === "SOC";

    const HIDE_ON_NOT_AUTOMATIC_OR_IS_GRIDSELL = (el: { mode: Mode; inputChannelAddressToggleValue: InputMode }) =>
        el.mode !== Mode.AUTOMATIC || el.inputChannelAddressToggleValue === "GRIDSELL";

    const HIDE_ON_NOT_AUTOMATIC_OR_NOT_SOC = (el: { mode: Mode; inputChannelAddressToggleValue: InputMode }) =>
        el.mode !== Mode.AUTOMATIC || el.inputChannelAddressToggleValue !== "SOC";

    const HIDE_ON_NOT_AUTOMATIC_OR_NOT_GRIDSELL = (el: { mode: Mode; inputChannelAddressToggleValue: InputMode }) =>
        el.mode !== Mode.AUTOMATIC || el.inputChannelAddressToggleValue !== "GRIDSELL";

    const HIDE_ON_NOT_AUTOMATIC_OR_NOT_GRIDBUY = (el: { mode: Mode; inputChannelAddressToggleValue: InputMode }) =>
        el.mode !== Mode.AUTOMATIC || el.inputChannelAddressToggleValue !== "GRIDBUY";

    const HIDE_ON_MODE_NOT_AUTOMATIC_OR_NOT_PRODUCTION = (el: {
        mode: Mode;
        inputChannelAddressToggleValue: InputMode;
    }) => el.mode !== Mode.AUTOMATIC || el.inputChannelAddressToggleValue !== "PRODUCTION";
}
