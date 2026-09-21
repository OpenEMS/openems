import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { SingleChartComponent } from "../../../Channelthreshold/history/chart/singlechart.component";
import { FlatComponent } from "./flat/flat";
import { ControllerIoChannelSingleThresholdHistoryComponent } from "./new-navigation/new-navigation";
import { ControllerIoChannelSingleThresholdOverviewComponent } from "./overview/overview";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        SingleChartComponent,
        ControllerIoChannelSingleThresholdHistoryComponent,
        FlatComponent,
        ControllerIoChannelSingleThresholdOverviewComponent,
    ],
    exports: [
        FlatComponent,
        ControllerIoChannelSingleThresholdOverviewComponent,
    ],
})
export class ControllerIoSingleThreshold {}
