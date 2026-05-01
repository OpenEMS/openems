import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { ControllerIoHeatingElementChartComponent } from "./chart/chart";
import { ControllerHeatingElementChartComponent } from "./flat/flat";
import { ControllerHeatingElementHistoryComponent } from "./new-navigation/new-navigation";
import { ControllerIoHeatingElementOverviewComponent } from "./overview/overview";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        ControllerHeatingElementHistoryComponent,
        ControllerIoHeatingElementChartComponent,
    ],
    declarations: [
        ControllerHeatingElementChartComponent,
        ControllerIoHeatingElementOverviewComponent,
    ],
    exports: [
        ControllerIoHeatingElementChartComponent,
        ControllerHeatingElementChartComponent,
        ControllerIoHeatingElementOverviewComponent,
        ControllerHeatingElementHistoryComponent,
    ],
})
export class ControllerHeatingElementHistory { }
