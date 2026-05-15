import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { ControllerIoHeatingElementChartComponent } from "./chart/chart";
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
        ControllerIoHeatingElementOverviewComponent,
    ],
    exports: [
        ControllerIoHeatingElementChartComponent,
        ControllerIoHeatingElementOverviewComponent,
        ControllerHeatingElementHistoryComponent,
    ],
})
export class ControllerHeatingElementHistory { }
