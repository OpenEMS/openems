import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { ChartComponent } from "./chart/chart";
import { FlatComponent } from "./flat/flat";
import { ControllerEssTimeOfUseTariffHistoryComponent } from "./new-navigation/new-navigation";
import { ControllerEssTimeOfUseTariffOverviewComponent } from "./overview/overview";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
    ],
    declarations: [
        FlatComponent,
        ControllerEssTimeOfUseTariffOverviewComponent,
        ChartComponent,
        ControllerEssTimeOfUseTariffHistoryComponent,
    ],
    exports: [
        FlatComponent,
        ControllerEssTimeOfUseTariffOverviewComponent,
        ChartComponent,
        ControllerEssTimeOfUseTariffHistoryComponent,
    ],
})
export class ControllerEssTimeOfUseTariffHistory { }
