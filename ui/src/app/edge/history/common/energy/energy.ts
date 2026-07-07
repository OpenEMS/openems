import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { FlatWidgetButtonComponent } from "src/app/shared/components/flat/flat-widget-button/flat-widget-button";
import { SharedModule } from "src/app/shared/shared.module";

import { ChartComponent } from "./chart/chart";
import { FlatComponent } from "./flat/flat";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        FlatWidgetButtonComponent,
        ChartComponent,
    ],
    declarations: [FlatComponent],
    exports: [ChartComponent, FlatComponent],
})
export class CommonEnergyMonitor {}
