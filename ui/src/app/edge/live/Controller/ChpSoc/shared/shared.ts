import { FormControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { NavigationConstants, NavigationTree } from "src/app/shared/components/navigation/shared";
import { Converter } from "src/app/shared/components/shared/converter";
import { Name } from "src/app/shared/components/shared/name";
import { OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, Edge, EdgeConfig } from "src/app/shared/shared";
import { Mode } from "src/app/shared/type/general";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { ControllerChpFlatComponent } from "../flat/ChpSoc";

export namespace SharedControllerChpSoc {
    const HIDE_ON_MODE_NOT_AUTOMATIC = (el: { mode: Mode }) => el.mode !== Mode.AUTOMATIC;

    export const getFormlyView = (
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
    ): OeFormlyView<ChpViewModel> => {
        return {
            title: component.alias,
            icon: { name: "flame-outline", color: "normal", size: "large" },
            helpKey: "REDIRECT.CONTROLLER_CHP_SOC",
            lines: [
                ...getFormlySettingsLines(translate),
                ...getFormlyAutomaticLines(translate, component),
                ...getFormlyStateLine(translate, component),
            ],
            component: component,
            edge: edge,
        };
    };

    const getFormlyAutomaticLines = (
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): OeFormlyView<ChpViewModel>["lines"] => {
        const lines: OeFormlyView<ChpViewModel>["lines"] = [];

        const inputChannelAddress = component.getPropertyFromComponent<string>("inputChannelAddress");
        AssertionUtils.assertIsDefined(inputChannelAddress);

        lines.push(
            {
                type: "horizontal-line",
            },
            {
                type: "channel-line",
                name: translate.instant("GENERAL.SOC"),
                channel: inputChannelAddress,
                converter: Converter.TO_PERCENT,
            },
            {
                type: "channel-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.CHP.HIGH_THRESHOLD"),
                channel: component.id + "/_PropertyHighThreshold",
                converter: Converter.TO_STRING,
            },
            {
                type: "channel-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.CHP.LOW_THRESHOLD"),
                channel: component.id + "/_PropertyLowThreshold",
                converter: Converter.TO_STRING,
            },
            {
                type: "dual-knob-range-button-from-form-control-line",
                lowerControlName: "lowThreshold",
                upperControlName: "highThreshold",
                properties: {
                    dualKnobs: true,
                    tickMin: 0,
                    tickMax: 100,
                    step: 1,
                    tickFormatter: (val) => Converter.STATE_IN_PERCENT(val),
                    pinFormatter: (val) => Converter.STATE_IN_PERCENT(val),
                },
            },
        );

        return lines.map((line) => ({
            ...line,
            hide: line.hide ?? HIDE_ON_MODE_NOT_AUTOMATIC,
        }));
    };

    const getFormlySettingsLines = (translate: TranslateService): OeFormlyView<ChpViewModel>["lines"] => {
        const lines: OeFormlyView<ChpViewModel>["lines"] = [];

        lines.push({
            type: "buttons-from-form-control-line",
            name: translate.instant("GENERAL.MODE"),
            controlName: "mode",
            buttons: [
                {
                    name: translate.instant("GENERAL.ON"),
                    value: Mode.MANUAL_ON,
                    icon: {
                        color: "success",
                        name: "play-outline",
                        size: "medium",
                    },
                },
                {
                    name: translate.instant("GENERAL.AUTOMATIC"),
                    value: Mode.AUTOMATIC,
                    icon: {
                        color: "primary",
                        name: "sunny",
                        size: "medium",
                    },
                },
                {
                    name: translate.instant("GENERAL.OFF"),
                    value: Mode.MANUAL_OFF,
                    icon: {
                        color: "danger",
                        name: "power-outline",
                        size: "medium",
                    },
                },
            ],
        });
        return lines;
    };

    const getFormlyStateLine = (
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): OeFormlyView<ChpViewModel>["lines"] => {
        const lines: OeFormlyView<ChpViewModel>["lines"] = [];

        const outputChannelAddress = component.getPropertyFromComponent<string>("outputChannelAddress");
        AssertionUtils.assertIsDefined(outputChannelAddress);

        lines.push(
            {
                type: "horizontal-line",
            },
            {
                type: "channel-line",
                name: translate.instant("GENERAL.STATE"),
                channel: outputChannelAddress,
                converter: SharedControllerChpSoc.CONVERT_CHP_STATE(translate),
            },
        );
        return lines;
    };

    export function getChannelAddresses(component: EdgeConfig.Component): Promise<ChannelAddress[]> {
        const outputChannel = ChannelAddress.fromStringSafely(
            component.getPropertyFromComponent("outputChannelAddress"),
        );
        const inputChannel = ChannelAddress.fromStringSafely(component.getPropertyFromComponent("inputChannelAddress"));
        const propertyModeChannel = new ChannelAddress(component.id, ControllerChpFlatComponent.PROPERTY_MODE);

        return Promise.resolve([
            ...(outputChannel ? [outputChannel] : []),
            ...(inputChannel ? [inputChannel] : []),
            propertyModeChannel,
            new ChannelAddress(component.id, "_PropertyHighThreshold"),
            new ChannelAddress(component.id, "_PropertyLowThreshold"),
        ]);
    }

    export function getFormGroup(): FormGroup {
        return new FormGroup({
            mode: new FormControl(null),
            lowThreshold: new FormControl(null),
            highThreshold: new FormControl(null),
        });
    }

    export function getNavigationTree(
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): ConstructorParameters<typeof NavigationTree> {
        return new NavigationTree(
            component.id,
            { baseString: "controller/chp/" + component.id },
            { name: "flame-outline", color: "normal" },
            Name.METER_ALIAS_OR_ID(component),
            "label",
            [NavigationConstants.CommonNodes.HISTORY(translate), NavigationConstants.CommonNodes.SETTINGS(translate)],
            null,
        ).toConstructorParams();
    }

    /**
     * Converts the runState of the heating element to the tranlsated state
     *
     * @param translate The current language to be translated to
     * @returns Converted value
     */
    export const CONVERT_CHP_STATE = (translate: TranslateService) => {
        return (value: any): string => {
            switch (value) {
                case 0:
                    return translate.instant("GENERAL.INACTIVE");
                case 1:
                    return translate.instant("GENERAL.ACTIVE");
                default:
                    return "?";
            }
        };
    };
}

export type ChpViewModel = {
    mode: Mode;
};
