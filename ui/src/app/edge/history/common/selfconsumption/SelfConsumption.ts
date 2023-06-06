import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { FlatComponent } from "./flat/flat";
import { NgModule } from "@angular/core";
import { ChartComponent } from "./chart/chart.component";
import { OverviewComponent } from "./overview/overview";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
    ],
    entryComponents: [
        FlatComponent,
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
    ]
})
export class Common_Selfconsumption { }