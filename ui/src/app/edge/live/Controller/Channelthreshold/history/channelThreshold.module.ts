import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { SingleChartComponent } from "./chart/singlechart.component";
import { TotalChartComponent } from "./chart/totalchart.component";
import { FlatComponent } from "./flat/flat";
import { ControllerChannelthresholdHistoryComponent } from "./new-navigation/new-navigation";
import { ControllerChannelThresholdOverviewComponent } from "./overview/overview";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        SingleChartComponent,
        ControllerChannelthresholdHistoryComponent,
    ],
    declarations: [
        FlatComponent,
        TotalChartComponent,
        ControllerChannelThresholdOverviewComponent,
    ],
    exports: [
        FlatComponent,
        TotalChartComponent,
        ControllerChannelThresholdOverviewComponent,
    ],
})
export class ChannelThreshold { }
