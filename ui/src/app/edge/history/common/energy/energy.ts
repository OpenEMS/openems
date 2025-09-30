import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/SHARED.MODULE";

import { ChartComponent } from "./chart/chart";
import { FlatComponent } from "./flat/flat";

@NgModule({
  imports: [
    BrowserModule,
    SharedModule,
  ],
  declarations: [
    FlatComponent,
    ChartComponent,
  ],
  exports: [
    FlatComponent,
  ],
})
export class CommonEnergyMonitor { }
