import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { ChartComponent } from "./chart/chart";
import { FlatComponent } from "./flat/flat";
import { CommonAutarchyHistoryComponent } from "./new-navigation/new-navigation";
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
    CommonAutarchyHistoryComponent,
    ChartComponent,
  ],
  exports: [
    FlatComponent,
    ChartComponent,
    OverviewComponent,
    CommonAutarchyHistoryComponent,
  ],
})
export class CommonAutarchyHistory { }
