import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { HistoryComponent } from './history.component';
import { EnergyComponent } from './energy/energy.component';
import { KwhComponent } from './kwh/kwh.component';
import { ChannelthresholdComponent } from './channelthreshold/chart.component';
import { EvcsChartComponent } from './evcs/chart.component';
import { ChpSocChartComponent } from './chpsoc/chart.component';
import { GridComponent } from './grid/widget.component';
import { ConsumptionComponent } from './consumption/widget.component';
import { StorageComponent } from './storage/widget.component';
import { ProductionComponent } from './production/widget.component';
import { EvcsWidgetComponent } from './evcs/widget.component';
import { SelfconsumptionWidgetComponent } from './selfconsumption/widget.component';
import { AutarchyWidgetComponent } from './autarchy/widget.component';
import { ChpSocWidgetComponent } from './chpsoc/widget.component';
import { StorageModalComponent } from './storage/modal/modal.component';
import { SelfconsumptionModalComponent } from './selfconsumption/modal/modal.component';
import { ProductionModalComponent } from './production/modal/modal.component';
import { GridModalComponent } from './grid/modal/modal.component';
import { EvcsModalComponent } from './evcs/modal/modal.component';
import { ConsumptionModalComponent } from './consumption/modal/modal.component';
import { ChpSocModalComponent } from './chpsoc/modal/modal.component';
import { AutarchyModalComponent } from './autarchy/modal/modal.component';
import { ChannelthresholdModalComponent } from './channelthreshold/modal/modal.component';
import { ChanneltresholdWidgetComponent } from './channelthreshold/widget.component';
import { AutarchyChartComponent } from './autarchy/chart.component';
import { SelfconsumptionChartComponent } from './selfconsumption/chart.component';
import { ProductionChartComponent } from './production/chart.component';
import { StorageChartComponent } from './storage/chart.component';
import { GridChartComponent } from './grid/chart.component';
import { ConsumptionChartComponent } from './consumption/chart.component';
import { SocStorageChartComponent } from './storage/socchart.component';
import { EnergyModalComponent } from './energy/modal/modal.component';

@NgModule({
  imports: [
    SharedModule,
  ],
  entryComponents: [
    StorageModalComponent,
    SelfconsumptionModalComponent,
    ProductionModalComponent,
    GridModalComponent,
    EvcsModalComponent,
    ConsumptionModalComponent,
    ChpSocModalComponent,
    ChannelthresholdModalComponent,
    AutarchyModalComponent,
    EnergyModalComponent
  ],
  declarations: [
    HistoryComponent,
    AutarchyChartComponent,
    EnergyComponent,
    KwhComponent,
    ChannelthresholdComponent,
    EvcsChartComponent,
    ChpSocChartComponent,
    ConsumptionComponent,
    GridComponent,
    StorageComponent,
    ProductionComponent,
    EvcsWidgetComponent,
    SelfconsumptionWidgetComponent,
    AutarchyWidgetComponent,
    ChpSocWidgetComponent,
    ChanneltresholdWidgetComponent,
    StorageModalComponent,
    SelfconsumptionModalComponent,
    ProductionModalComponent,
    GridModalComponent,
    EvcsModalComponent,
    ConsumptionModalComponent,
    ChpSocModalComponent,
    ChannelthresholdModalComponent,
    AutarchyModalComponent,
    StorageChartComponent,
    SelfconsumptionChartComponent,
    ProductionChartComponent,
    GridChartComponent,
    ConsumptionChartComponent,
    SocStorageChartComponent,
    EnergyModalComponent
  ]
})
export class HistoryModule { }
