import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { ModalModule } from "src/app/shared/components/modal/modal.module";
import { SharedModule } from "src/app/shared/shared.module";
import { FlatComponent } from "./flat/flat";
import { ControllerEssTimeOfUseTariffHistory } from "./history/timeOfUseTariff.module";
import { ModalComponent } from "./modal/modal";
import { ControllerEssTimeOfUseTariffHomeComponent } from "./new-navigation/new-navigation";
import { SchedulePowerAndSocChartComponent } from "./new-navigation/power-soc-chart";
import { ScheduleStateAndPriceChartComponent } from "./new-navigation/state-price-chart";
import { ControllerEssTimeOfUseTariffSettingsComponent } from "./settings/settings";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        ModalModule,
        ControllerEssTimeOfUseTariffHistory,
        ControllerEssTimeOfUseTariffHomeComponent,
        ControllerEssTimeOfUseTariffSettingsComponent,
    ],
    declarations: [
        ModalComponent,
        FlatComponent,
        SchedulePowerAndSocChartComponent,
        ScheduleStateAndPriceChartComponent,
    ],
    exports: [
        FlatComponent,
        ControllerEssTimeOfUseTariffHomeComponent,
        ControllerEssTimeOfUseTariffSettingsComponent,
    ],
})
export class ControllerEssTimeOfUseTariff { }
