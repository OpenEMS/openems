import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { SharedModule } from "./../../../shared/SHARED.MODULE";
import { EnergymonitorChartComponent } from "./chart/CHART.COMPONENT";
import { ConsumptionSectionComponent } from "./chart/section/CONSUMPTION.COMPONENT";
import { GridSectionComponent } from "./chart/section/GRID.COMPONENT";
import { ProductionSectionComponent } from "./chart/section/PRODUCTION.COMPONENT";
import { StorageSectionComponent } from "./chart/section/STORAGE.COMPONENT";
import { EnergymonitorComponent } from "./ENERGYMONITOR.COMPONENT";

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
