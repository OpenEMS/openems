import { NgModule } from '@angular/core';
import { SharedModule } from './../../../shared/shared.module';

import { EnergymonitorComponent } from './energymonitor.component';
import { EnergymonitorChartComponent } from './chart/chart.component';
import { ConsumptionSectionComponent } from './chart/section/consumption.component';
import { GridSectionComponent } from './chart/section/grid.component';
import { ProductionSectionComponent } from './chart/section/production.component';
import { StorageSectionComponent } from './chart/section/storage.component';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

@NgModule({
  imports: [
    SharedModule,
    BrowserModule,
    BrowserAnimationsModule
  ],
  declarations: [
    EnergymonitorComponent,
    EnergymonitorChartComponent,
    ConsumptionSectionComponent,
    ProductionSectionComponent,
    GridSectionComponent,
    StorageSectionComponent,
  ],
  exports: [
    EnergymonitorComponent
  ]
})
export class EnergymonitorModule { }



