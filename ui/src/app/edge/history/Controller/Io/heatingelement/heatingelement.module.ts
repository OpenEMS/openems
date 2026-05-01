import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { ChartComponent } from "./chart/chart";
import { FlatComponent } from "./flat/flat";
import { ControllerIoHeatingElementOverviewComponent } from "./overview/overview";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
    ],
    declarations: [
        ChartComponent,
        FlatComponent,
        ControllerIoHeatingElementOverviewComponent,
    ],
    exports: [
        ChartComponent,
        FlatComponent,
        ControllerIoHeatingElementOverviewComponent,
    ],
})
export class HeatingElement { }
