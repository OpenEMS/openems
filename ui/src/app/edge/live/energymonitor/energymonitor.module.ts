import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { SharedModule } from './../../../shared/shared.module';

import { ConsumptionSectionComponent } from './chart/section/consumption.component';
import { EnergymonitorChartComponent } from './chart/chart.component';
import { EnergymonitorComponent } from './energymonitor.component';
import { GridSectionComponent } from './chart/section/grid.component';
import { ProductionSectionComponent } from './chart/section/production.component';
import { StorageSectionComponent } from './chart/section/storage.component';

@NgModule({
  imports: [
    BrowserAnimationsModule,
    BrowserModule,
    SharedModule
  ],
  declarations: [
    ConsumptionSectionComponent,
    EnergymonitorChartComponent,
    EnergymonitorComponent,
    GridSectionComponent,
    ProductionSectionComponent,
    StorageSectionComponent
  ],
  exports: [
    EnergymonitorComponent
  ]
})
export class EnergymonitorModule { }



