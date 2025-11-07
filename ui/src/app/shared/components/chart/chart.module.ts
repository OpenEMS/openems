import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { RouterModule } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerComponent, NgxSpinnerModule } from "ngx-spinner";

import { PipeComponentsModule, PipeModule } from "../../pipe/pipe.module";
import { FlatWidgetButtonComponent } from "../flat/flat-widget-button/flat-widget-button";
import { HistoryDataErrorModule } from "../history-data-error/history-data-error.module";
import { PickdateComponentModule, PickdateModule } from "../pickdate/pickdate.module";
import { ChartComponent } from "./chart";
import { ChartLegendComponent } from "./legend/legend";

@NgModule({
  imports: [
    IonicModule,
    PipeComponentsModule,
    TranslateModule,
    BaseChartDirective,
    CommonModule,
    NgxSpinnerModule.forRoot({
      type: "ball-clip-rotate-multiple",
    }),
    HistoryDataErrorModule,
    RouterModule,
    PickdateComponentModule,
    FlatWidgetButtonComponent,
  ],
  declarations: [
    ChartComponent,
    ChartLegendComponent,
  ],
  exports: [
    ChartComponent,
    ChartLegendComponent,
    BaseChartDirective,
    NgxSpinnerComponent,
  ],
})
export class ChartComponentsModule { }

/**
* @deprecated should avoid creating modules with browsermodule imported
*/
@NgModule({
  imports: [
    BrowserModule,
    ChartComponentsModule,
    PipeModule,
    PickdateModule,
  ],
  exports: [
    ChartComponentsModule,
    PickdateModule,
    PipeModule,
  ],
})
export class ChartModule { }

