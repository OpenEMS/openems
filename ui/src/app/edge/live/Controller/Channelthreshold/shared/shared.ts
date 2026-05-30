import { TranslateService } from "@ngx-translate/core";
import { NavigationTree } from "src/app/shared/components/navigation/shared";
import { Name } from "src/app/shared/components/shared/name";
import { EdgeConfig } from "src/app/shared/shared";

export namespace SharedControllerChannelThreshold {
    export function getNavigationTree(translate: TranslateService, component: EdgeConfig.Component): ConstructorParameters<typeof NavigationTree> {
        return new NavigationTree(component.id, { baseString: "controller/channelthreshold/" + component.id }, { name: "radio-button-on-outline", color: "normal" }, Name.METER_ALIAS_OR_ID(component), "label", [
            new NavigationTree("history", { baseString: "history" }, { name: "stats-chart-outline", color: "warning" }, translate.instant("GENERAL.HISTORY"), "label", [], null),
        ], null).toConstructorParams();
    }
}
