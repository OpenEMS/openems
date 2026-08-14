import { TranslateService } from "@ngx-translate/core";
import { TextIndentation } from "src/app/shared/components/modal/modal-line/modal-line";
import { NavigationConstants, NavigationTree, } from "src/app/shared/components/navigation/shared";
import { Converter } from "src/app/shared/components/shared/converter";
import { Name } from "src/app/shared/components/shared/name";
import { OeFormlyField } from "src/app/shared/components/shared/oe-formly-component";
import { Phase } from "src/app/shared/components/shared/phase";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";

export namespace SharedControllerPeakShavingAsymmetric {
    export function getNavigationTree(
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): ConstructorParameters<typeof NavigationTree> {
        return new NavigationTree(
            component.id,
            {
                baseString:
                    "controller/peak-shaving-asymmetric/" + component.id,
            },
            { name: "trending-down-outline", color: "normal" },
            Name.METER_ALIAS_OR_ID(component),
            "label",
            [
                NavigationConstants.CommonNodes.HISTORY(translate),
                NavigationConstants.CommonNodes.SETTINGS(translate),
            ],
            null,
        ).toConstructorParams();
    }

    export function generatePhasesView(
        component: EdgeConfig.Component,
        translate: TranslateService,
    ): OeFormlyField[] {
        const meterId = component.getPropertyFromComponent<string>("meter.id");

        if (meterId == null) {
            return [];
        }

        return Phase.THREE_PHASE.map(
            (phase) =>
                <OeFormlyField>{
                    type: "channel-line",
                    name:
                        translate.instant("GENERAL.MEASURED_VALUE") +
                        " " +
                        phase,
                    channel: new ChannelAddress(
                        meterId,
                        "ActivePower" + phase,
                    ).toString(),
                    indentation: TextIndentation.SINGLE,
                    converter: Converter.GRID_BUY_POWER_OR_ZERO,
                },
        );
    }
}
