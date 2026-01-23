import { TranslateService } from "@ngx-translate/core";
import { TextIndentation } from "src/app/shared/components/modal/modal-line/modal-line";
import { NavigationTree } from "src/app/shared/components/navigation/shared";
import { Converter } from "src/app/shared/components/shared/converter";
import { Filter } from "src/app/shared/components/shared/filter";
import { Name } from "src/app/shared/components/shared/name";
import { OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { Phase } from "src/app/shared/components/shared/phase";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, RippleControlReceiverRestrictionLevel } from "src/app/shared/shared";
import { ChartAnnotationState } from "src/app/shared/type/general";
import { Role } from "src/app/shared/type/role";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { GridSectionComponent } from "../../../energymonitor/chart/section/grid.component";
import { transformRcrValues } from "../history/shared-grid";

export namespace SharedGrid {

    export function getFormlyView(config: EdgeConfig | null, role: Role, translate: TranslateService): OeFormlyView {

        AssertionUtils.assertIsDefined(config);

        // Grid-Mode
        const lines: OeFormlyField[] = [{
            type: "channel-line",
            name: translate.instant("GENERAL.OFF_GRID"),
            channel: "_sum/GridMode",
            filter: Filter.GRID_MODE_IS_OFF_GRID,
            converter: Converter.HIDE_VALUE,
        }];

        const gridMeters = Object.values(config.components).filter(component => config?.isTypeGrid(component));

        // Sum Channels (if more than one meter)
        if (gridMeters.length > 1) {
            getLines(config, translate, lines);

            lines.push(
                {
                    type: "channel-line",
                    name: translate.instant("GENERAL.GRID_SELL_ADVANCED"),
                    channel: "_sum/GridActivePower",
                    converter: Converter.GRID_SELL_POWER_OR_ZERO,
                },
                {
                    type: "channel-line",
                    name: translate.instant("GENERAL.GRID_BUY_ADVANCED"),
                    channel: "_sum/GridActivePower",
                    converter: Converter.GRID_BUY_POWER_OR_ZERO,
                },
                {
                    type: "horizontal-line",
                },
            );
        }


        // Individual Meters
        for (const meter of gridMeters) {
            if (gridMeters.length === 1) {
                // Two lines if there is only one meter (= same visualization as with Sum Channels)
                getLines(config, translate, lines);

                lines.push(
                    {
                        type: "channel-line",
                        name: translate.instant("GENERAL.GRID_SELL_ADVANCED"),
                        channel: meter.id + "/ActivePower",
                        converter: Converter.GRID_SELL_POWER_OR_ZERO,
                    },
                    {
                        type: "channel-line",
                        name: translate.instant("GENERAL.GRID_BUY_ADVANCED"),
                        channel: meter.id + "/ActivePower",
                        converter: Converter.GRID_BUY_POWER_OR_ZERO,
                    },
                );

            } else {
                // More than one meter? Show only one line per meter.
                lines.push({
                    type: "channel-line",
                    name: Name.SUFFIX_FOR_GRID_SELL_OR_GRID_BUY(translate, meter.alias),
                    channel: meter.id + "/ActivePower",
                    converter: Converter.POWER_IN_WATT,
                });
            }

            lines.push(
                // Individual phases: Voltage, Current and Power
                ...generatePhasesView(meter, translate, role),
                {
                    // Line separator
                    type: "horizontal-line",
                },
            );
        }

        if (gridMeters.length > 0) {
            // Technical info
            lines.push({
                type: "info-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.PHASES_INFO"),
            });
        }

        return {
            title: translate.instant("GENERAL.GRID"),
            lines: lines,
            helpKey: "REDIRECT.COMMON_GRID",
            component: new EdgeConfig.Component(),
            isCommonWidget: "true",
        };
    }

    export function generatePhasesView(component: EdgeConfig.Component, translate: TranslateService, role: Role): OeFormlyField[] {
        return Phase.THREE_PHASE
            .map(phase => <OeFormlyField>{
                type: "children-line",
                name: {
                    channel: ChannelAddress.fromString(component.id + "/ActivePower" + phase),
                    converter: Name.SUFFIX_FOR_GRID_SELL_OR_GRID_BUY(translate, translate.instant("GENERAL.PHASE") + " " + phase),
                },

                indentation: TextIndentation.SINGLE,
                children: generatePhasesLineItems(role, phase, component),
            });
    }

    export function generatePhasesLineItems(role: Role, phase: string, component: EdgeConfig.Component) {
        const children: OeFormlyField[] = [];
        if (Role.isAtLeast(role, Role.INSTALLER)) {
            children.push({
                type: "item",
                channel: component.id + "/Voltage" + phase,
                converter: Converter.VOLTAGE_IN_MILLIVOLT_TO_VOLT,
            }, {
                type: "item",
                channel: component.id + "/Current" + phase,
                converter: Converter.CURRENT_IN_MILLIAMPERE_TO_ABSOLUTE_AMPERE,
            });
        }

        children.push({
            type: "item",
            channel: component.id + "/ActivePower" + phase,
            converter: Converter.POSITIVE_POWER_IN_W,
        });

        return children;
    }

    export function getLines(config: EdgeConfig, translate: TranslateService, lines: OeFormlyField[]) {
        const is14aEnabled = GridSectionComponent.isControllerEnabled(config, "Controller.Ess.Limiter14a");
        const limiter14aValue = "4,2 kW";
        const isRcrEnabled = GridSectionComponent.isControllerEnabled(config, "Controller.Ess.RippleControlReceiver");

        const controller14a = config.getComponentIdsByFactory("Controller.Ess.Limiter14a")?.[0] ?? null;
        const controllerRcr = config.getComponentIdsByFactory("Controller.Ess.RippleControlReceiver")?.[0] ?? null;

        lines.push({
            type: "value-from-channels-line",
            name: translate.instant("GENERAL.STATE"),
            value: (currentData: CurrentData) => Converter.GRID_STATE_TO_MESSAGE(translate, currentData),
            channelsToSubscribe: [
                ...getChannelsFromController(config),
            ],
        });

        if (isRcrEnabled) {
            lines.push({
                type: "value-from-channels-line",
                name: translate.instant("GRID_STATES.FEED_IN_LIMITATION"),
                value: (currentData: CurrentData) => {
                    const value = transformRcrValues(currentData.allComponents[controllerRcr + "/RestrictionMode"]) ?? 0;
                    return value + " % (" + translate.instant("GRID_STATES.RIPPLE_CONTROL_RECEIVER") + " " + (100 - value) + "%" + ")";
                },
                channelsToSubscribe: [
                    new ChannelAddress(controllerRcr, "RestrictionMode"),
                ],
                filter: (currentData: CurrentData) => {
                    const restrictionMode = currentData?.allComponents[controllerRcr + "/RestrictionMode"] ?? null;
                    if (restrictionMode == null) {
                        return true;
                    }
                    return restrictionMode !== RippleControlReceiverRestrictionLevel.NO_RESTRICTION;
                },
            });
        }
        if (is14aEnabled) {
            lines.push({
                type: "value-from-channels-line",
                name: translate.instant("GRID_STATES.FEED_IN_DESCRIPITON"),
                value: (currentData: CurrentData) => currentData.allComponents[controller14a + "/RestrictionMode"] == ChartAnnotationState.ON ? limiter14aValue : "-",
                channelsToSubscribe: [
                    new ChannelAddress(controller14a, "RestrictionMode"),
                ],
                filter: (currentData: CurrentData) => {
                    const restrictionMode = currentData?.allComponents[controller14a + "/RestrictionMode"] ?? null;
                    if (restrictionMode == null) {
                        return true;
                    }
                    return restrictionMode !== ChartAnnotationState.OFF;
                },
            });
        }

        if (is14aEnabled || isRcrEnabled) {
            lines.push({
                type: "horizontal-line",
            });
        }
    }

    export function getChannelsFromController(config: EdgeConfig): ChannelAddress[] {

        const channelAddresses: ChannelAddress[] = [new ChannelAddress("_sum", "GridMode")];
        const is14aActivated = GridSectionComponent.isControllerEnabled(config, "Controller.Ess.Limiter14a");
        const isRcrActivated = GridSectionComponent.isControllerEnabled(config, "Controller.Ess.RippleControlReceiver");
        const controller14a = config.getComponentIdsByFactory("Controller.Ess.Limiter14a")[0];
        const controllerRcr = config.getComponentIdsByFactory("Controller.Ess.RippleControlReceiver")[0];
        if (is14aActivated) {
            channelAddresses.push(new ChannelAddress(controller14a, "RestrictionMode"));
        }

        if (isRcrActivated) {
            channelAddresses.push(new ChannelAddress(controllerRcr, "RestrictionMode"));
        }
        return channelAddresses;
    }

    export function getNavigationTree(edge: Edge, config: EdgeConfig, translate: TranslateService): ConstructorParameters<typeof NavigationTree> | null {
        const gridMeters = Object.values(config.components)
            .filter((component) => component.isEnabled && config.isTypeGrid(component));

        if (gridMeters == null) {
            return null;
        }

        return new NavigationTree("grid", { baseString: "common/grid" }, { name: "oe-grid", color: "dark" }, translate.instant("GENERAL.GRID"), "label", [
            new NavigationTree("history", { baseString: "history" }, { name: "stats-chart-outline", color: "warning" }, translate.instant("GENERAL.HISTORY"), "label", [
                ...gridMeters
                    .map(el => new NavigationTree(el.id + "/phase-accurate", { baseString: el.id + "/phase-accurate" },
                        { name: "oe-grid", color: "dark" }, gridMeters.length === 1 ? translate.instant("EDGE.HISTORY.PHASE_ACCURATE") : el.alias, "label",
                        edge.roleIsAtLeast(Role.INSTALLER)
                            ? [new NavigationTree("current-voltage", { baseString: "current-voltage" }, { name: "flame", color: "danger" }, translate.instant("EDGE.HISTORY.CURRENT_AND_VOLTAGE"), "label", [], null)]
                            : [],
                        null)),
                new NavigationTree(
                    "external-limitation", { baseString: "external-limitation" }, { name: "flame", color: "danger" }, translate.instant("EDGE.HISTORY.EXTERNAL_LIMITATION"), "label", [],
                    null),
            ], null),
        ], null).toConstructorParams();
    }
}
