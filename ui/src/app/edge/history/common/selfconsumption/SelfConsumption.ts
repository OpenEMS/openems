import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/SHARED.MODULE";
import { ChartComponent } from "./chart/CHART.COMPONENT";
import { FlatComponent } from "./flat/flat";
import { OverviewComponent } from "./overview/overview";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
    ],
    declarations: [
        FlatComponent,
        ChartComponent,
        OverviewComponent,
    ],
    exports: [
        FlatComponent,
        ChartComponent,
        OverviewComponent,
    ],
})
export class Common_Selfconsumption { }
