import { NgModule } from '@angular/core';
import { SharedModule } from './../../../shared/shared.module';

import { EnergymonitorComponent } from './energymonitor.component';
import { EnergymonitorChartComponent } from './chart/chart.component';
import { ConsumptionSectionComponent } from './chart/section/consumptionsection.component';
import { GridSectionComponent } from './chart/section/gridsection.component';
import { ProductionSectionComponent } from './chart/section/productionsection.component';
import { StorageSectionComponent } from './chart/section/storagesection.component';
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



