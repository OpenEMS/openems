import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/SHARED.MODULE";
import { TotalChartComponent } from "./chart/TOTALCHART.COMPONENT";
import { FlatComponent } from "./flat/flat";
import { OverviewComponent } from "./overview/overview";

@NgModule({
  imports: [
    BrowserModule,
    SharedModule,
  ],
  declarations: [
    FlatComponent,
    TotalChartComponent,
    OverviewComponent,
  ],
  exports: [
    FlatComponent,
    TotalChartComponent,
    OverviewComponent,
  ],
})
export class ChannelThreshold { }
