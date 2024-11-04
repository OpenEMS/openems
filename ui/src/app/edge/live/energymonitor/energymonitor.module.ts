import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { SharedModule } from "./../../../shared/shared.module";
import { EnergymonitorChartComponent } from "./chart/chart.component";
import { ConsumptionSectionComponent } from "./chart/section/consumption.component";
import { GridSectionComponent } from "./chart/section/grid.component";
import { ProductionSectionComponent } from "./chart/section/production.component";
import { StorageSectionComponent } from "./chart/section/storage.component";
import { EnergymonitorComponent } from "./energymonitor.component";

@NgModule({
  imports: [
    BrowserAnimationsModule,
    BrowserModule,
    SharedModule,
  ],
  declarations: [
    ConsumptionSectionComponent,
    EnergymonitorChartComponent,
    EnergymonitorComponent,
    GridSectionComponent,
    ProductionSectionComponent,
    StorageSectionComponent,
  ],
  exports: [
    EnergymonitorComponent,
  ],
})
export class EnergymonitorModule { }
