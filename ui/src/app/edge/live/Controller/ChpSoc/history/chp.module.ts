import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { ControllerChpChartComponent } from "./chart/chart";
import { FlatComponent } from "./flat/flat";
import { ControllerChpHistoryComponent } from "./new-navigation/new-navigation";
import { ControllerChpOverviewComponent } from "./overview/overview";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        ControllerChpHistoryComponent,
        ControllerChpChartComponent,
        FlatComponent,
        ControllerChpOverviewComponent,
    ],
    exports: [
        FlatComponent,
        ControllerChpChartComponent,
        ControllerChpOverviewComponent,
        ControllerChpHistoryComponent,
    ],
})
export class ControllerChpHistory {}
