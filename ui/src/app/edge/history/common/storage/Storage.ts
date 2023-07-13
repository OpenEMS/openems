import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { SocChartComponent } from "./chart/socChart";
import { FlatComponent } from "./flat/flat";
import { OverviewComponent } from "./overview/overview";
import { TotalChartComponent } from "./chart/totalChart";
import { SingleChartComponent } from "./chart/singlechart";
import { EssChartComponent } from "./chart/esschart";
import { ChargerChartComponent } from "./chart/chargerchart";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule
    ],
    entryComponents: [
        FlatComponent,
        OverviewComponent,
        SocChartComponent,
        TotalChartComponent,
        SingleChartComponent,
        EssChartComponent,
        ChargerChartComponent
    ],
    declarations: [
        FlatComponent,
        OverviewComponent,
        SocChartComponent,
        TotalChartComponent,
        SingleChartComponent,
        EssChartComponent,
        ChargerChartComponent
    ],
    exports: [
        FlatComponent,
        OverviewComponent,
        SocChartComponent,
        TotalChartComponent,
        SingleChartComponent,
        EssChartComponent,
        ChargerChartComponent
    ]
})
export class Common_Storage { }