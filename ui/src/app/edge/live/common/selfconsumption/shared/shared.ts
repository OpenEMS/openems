import { FormControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { NavigationTree } from "src/app/shared/components/navigation/shared";
import { OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, EdgeConfig, Utils } from "src/app/shared/shared";

export namespace SharedSelfConsumption {

    export const COMMON_NOTE = (translate: TranslateService): OeFormlyField.InfoLine => ({
        type: "info-line",
        name: translate.instant("EDGE.INDEX.WIDGETS.SELFCONSUMPTION_INFO"),
    });

    export const getFormlyView = (translate: TranslateService): OeFormlyView => ({
        title: translate.instant("GENERAL.SELF_CONSUMPTION"),
        helpKey: "REDIRECT.COMMON_SELFCONSUMPTION",
        lines: [
            {
                type: "percentage-bar-line",
                controlName: "selfConsumption",
            },
            COMMON_NOTE(translate),
        ],
        component: new EdgeConfig.Component(),
    });

    export function getChannelAddresses(): ChannelAddress[] {
        return [
            new ChannelAddress("_sum", "GridActivePower"),
            new ChannelAddress("_sum", "ProductionActivePower"),
        ];
    }

    export function getSelfConsumptionValue(currentData: CurrentData) {
        return Utils.calculateSelfConsumption(
            Utils.multiplySafely(
                currentData.allComponents["_sum/GridActivePower"],
                -1,
            ),
            currentData.allComponents["_sum/ProductionActivePower"]);
    }

    export function getFormGroup(): FormGroup {
        return new FormGroup({
            selfConsumption: new FormControl(null),
        });
    }


    export function getNavigationTree(translate: TranslateService): ConstructorParameters<typeof NavigationTree> {
        return new NavigationTree("selfconsumption", { baseString: "common/selfconsumption" }, { name: "oe-consumption", color: "normal" }, translate.instant("GENERAL.SELF_CONSUMPTION"), "label", [
            new NavigationTree("history", { baseString: "history" }, { name: "stats-chart-outline", color: "warning" }, translate.instant("GENERAL.HISTORY"), "label", [], null),
        ], null).toConstructorParams();
    }
}
