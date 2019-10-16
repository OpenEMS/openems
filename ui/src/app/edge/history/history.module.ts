import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { HistoryComponent } from './history.component';
import { EnergyComponent } from './energy/energy.component';
import { KwhComponent } from './kwh/kwh.component';
import { ChannelthresholdComponent } from './channelthreshold/channelthreshold.component';
import { EvcsChartComponent } from './evcs/chart.component';
import { ExportComponent } from './export/export.component';
import { ChpSocChartComponent } from './chpsoc/chart.component';
import { GridComponent } from './grid/grid.component';
import { ConsumptionComponent } from './consumption/consumption.component';
import { StorageComponent } from './storage/storage.component';
import { ProductionComponent } from './production/production.component';
import { EvcsWidgetComponent } from './evcs/widget.component';
import { SelfconsumptionWidgetComponent } from './selfconsumption/widget.component';
import { AutarchyWidgetComponent } from './autarchy/widget.component';
import { ChpSocWidgetComponent } from './chpsoc/widget.component';

@NgModule({
  imports: [
    SharedModule,
  ],
  declarations: [
    HistoryComponent,
    EnergyComponent,
    KwhComponent,
    ChannelthresholdComponent,
    EvcsChartComponent,
    ExportComponent,
    ChpSocChartComponent,
    ConsumptionComponent,
    GridComponent,
    StorageComponent,
    ProductionComponent,
    EvcsWidgetComponent,
    SelfconsumptionWidgetComponent,
    AutarchyWidgetComponent,
    ChpSocWidgetComponent
  ]
})
export class HistoryModule { }
