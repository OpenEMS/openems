import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { ChartComponent } from "./chart/chart";
import { FlatComponent } from "./flat/flat";
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
    ],
    exports: [
        FlatComponent,
        ControllerEssTimeOfUseTariffOverviewComponent,
        ChartComponent,
    ],
})
export class TimeOfUseTariff { }
