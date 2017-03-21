import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MaterialModule } from '@angular/material';
import { FlexLayoutModule } from '@angular/flex-layout';

import { EnergymonitorComponent } from './energymonitor.component';
import { EnergymonitorChartComponent } from './chart/chart.component';
import { ConsumptionSectionComponent } from './chart/section/consumptionsection.component';
import { GridSectionComponent } from './chart/section/gridsection.component';
import { ProductionSectionComponent } from './chart/section/productionsection.component';
import { StorageSectionComponent } from './chart/section/storagesection.component';

@NgModule({
  imports: [
    CommonModule,
    MaterialModule.forRoot(),
    FlexLayoutModule.forRoot()
  ],
  declarations: [
    EnergymonitorComponent,
    EnergymonitorChartComponent,
    ConsumptionSectionComponent,
    ProductionSectionComponent,
    GridSectionComponent,
    StorageSectionComponent
  ],
  exports: [EnergymonitorComponent]
})
export class DeviceOverviewEnergymonitorModule { }



