import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { ChartComponent } from "./chart/chart";
import { FlatComponent } from "./flat/flat";
import { ControllerHeatHistoryComponent } from "./new-navigation/new-navigation";
import { ControllerHeatOverviewComponent } from "./overview/overview";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
    ],
    declarations: [
        FlatComponent,
        ControllerHeatHistoryComponent,
        ChartComponent,
        ControllerHeatOverviewComponent,
    ],
    exports: [
        FlatComponent,
        ChartComponent,
        ControllerHeatHistoryComponent,
        ControllerHeatOverviewComponent,
    ],
})
export class ControllerHeat { }
