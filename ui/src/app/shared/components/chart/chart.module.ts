import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { RouterModule } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { NgChartsModule } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";

import { PipeModule } from "../../pipe/pipe";
import { HistoryDataErrorModule } from "../history-data-error/history-data-error.module";
import { PickdateModule } from "../pickdate/pickdate.module";
import { ChartComponent } from "./chart";

@NgModule({
  imports: [
    BrowserModule,
    IonicModule,
    PipeModule,
    TranslateModule,
    NgChartsModule,
    CommonModule,
    NgxSpinnerModule.forRoot({
      type: "ball-clip-rotate-multiple",
    }),
    HistoryDataErrorModule,
    RouterModule,
    PickdateModule,
  ],
  declarations: [
    ChartComponent,
  ],
  exports: [
    ChartComponent,
  ],
})
export class ChartModule { }
