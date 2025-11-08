import { FormControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { NavigationTree } from "src/app/shared/components/navigation/shared";
import { OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, EdgeConfig, Utils } from "src/app/shared/shared";

export namespace SharedAutarchy {

    export const COMMON_NOTE = (translate: TranslateService): OeFormlyField.InfoLine => ({
        type: "info-line",
        name: translate.instant("EDGE.INDEX.WIDGETS.AUTARCHY_INFO"),
    });

    export const getFormlyView = (translate: TranslateService): OeFormlyView => ({
        title: translate.instant("GENERAL.AUTARCHY"),
        helpKey: "REDIRECT.COMMON_AUTARCHY",
        lines: [
            {
                type: "percentage-bar-line",
                controlName: "autarchy",
            },
            COMMON_NOTE(translate),
        ],
        component: new EdgeConfig.Component(),
    });


    export function getChannelAddresses(): ChannelAddress[] {
        return [
            new ChannelAddress("_sum", "GridActivePower"),
            new ChannelAddress("_sum", "ConsumptionActivePower"),
        ];
    }

    export function getAutarchyValue(currentData: CurrentData) {

        return Utils.calculateAutarchy(
            currentData.allComponents["_sum/GridActivePower"],
            currentData.allComponents["_sum/ConsumptionActivePower"],
        );
    }

    export function getFormGroup(): FormGroup {
        return new FormGroup({
            autarchy: new FormControl(null),
        });
    }

    export function getNavigationTree(translate: TranslateService): ConstructorParameters<typeof NavigationTree> {
        return new NavigationTree("autarchy", { baseString: "common/autarchy" }, { name: "oe-grid", color: "normal" }, translate.instant("GENERAL.AUTARCHY"), "label", [
            new NavigationTree("history", { baseString: "history" }, { name: "stats-chart-outline", color: "warning" }, translate.instant("GENERAL.HISTORY"), "label", [], null),
        ], null).toConstructorParams();
    }
}
