import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { AutarchyChartComponent } from './autarchy/chart.component';
import { AutarchyModalComponent } from './autarchy/modal/modal.component';
import { AutarchyWidgetComponent } from './autarchy/widget.component';
import { ChannelthresholdModalComponent } from './channelthreshold/modal/modal.component';
import { ChannelthresholdSingleChartComponent } from './channelthreshold/singlechart.component';
import { ChannelthresholdTotalChartComponent } from './channelthreshold/totalchart.component';
import { ChanneltresholdWidgetComponent } from './channelthreshold/widget.component';
import { ConsumptionEvcsChartComponent } from './consumption/evcschart.component';
import { ConsumptionModalComponent } from './consumption/modal/modal.component';
import { ConsumptionOtherChartComponent } from './consumption/otherchart.component';
import { ConsumptionSingleChartComponent } from './consumption/singlechart.component';
import { ConsumptionTotalChartComponent } from './consumption/totalchart.component';
import { ConsumptionComponent } from './consumption/widget.component';
import { EnergyComponent } from './energy/energy.component';
import { EnergyModalComponent } from './energy/modal/modal.component';
import { EvcsChartComponent } from './evcs/chart.component';
import { EvcsModalComponent } from './evcs/modal/modal.component';
import { EvcsWidgetComponent } from './evcs/widget.component';
import { GridChartComponent } from './grid/chart.component';
import { GridModalComponent } from './grid/modal/modal.component';
import { GridComponent } from './grid/widget.component';
import { HistoryComponent } from './history.component';
import { ProductionChargerChartComponent } from './production/chargerchart.component';
import { ProductionModalComponent } from './production/modal/modal.component';
import { ProductionMeterChartComponent } from './production/productionmeterchart';
import { ProductionSingleChartComponent } from './production/singlechart';
import { ProductionTotalAcChartComponent } from './production/totalacchart';
import { ProductionTotalChartComponent } from './production/totalchart';
import { ProductionTotalDcChartComponent } from './production/totaldcchart';
import { ProductionComponent } from './production/widget.component';
import { SelfconsumptionChartComponent } from './selfconsumption/chart.component';
import { SelfconsumptionModalComponent } from './selfconsumption/modal/modal.component';
import { SelfconsumptionWidgetComponent } from './selfconsumption/widget.component';
import { StorageChargerChartComponent } from './storage/chargerchart.component';
import { StorageESSChartComponent } from './storage/esschart.component';
import { StorageModalComponent } from './storage/modal/modal.component';
import { StorageSingleChartComponent } from './storage/singlechart.component';
import { SocStorageChartComponent } from './storage/socchart.component';
import { StorageTotalChartComponent } from './storage/totalchart.component';
import { StorageComponent } from './storage/widget.component';

@NgModule({
  imports: [
    SharedModule,
  ],
  entryComponents: [
    AutarchyModalComponent,
    ChannelthresholdModalComponent,
    ConsumptionModalComponent,
    EnergyModalComponent,
    EvcsModalComponent,
    GridModalComponent,
    ProductionModalComponent,
    SelfconsumptionModalComponent,
    StorageModalComponent,
  ],
  declarations: [
    AutarchyChartComponent,
    AutarchyModalComponent,
    AutarchyWidgetComponent,
    ChannelthresholdModalComponent,
    ChannelthresholdSingleChartComponent,
    ChannelthresholdTotalChartComponent,
    ChanneltresholdWidgetComponent,
    ConsumptionComponent,
    ConsumptionEvcsChartComponent,
    ConsumptionModalComponent,
    ConsumptionOtherChartComponent,
    ConsumptionSingleChartComponent,
    ConsumptionTotalChartComponent,
    EnergyComponent,
    EnergyModalComponent,
    EvcsChartComponent,
    EvcsModalComponent,
    EvcsWidgetComponent,
    GridChartComponent,
    GridComponent,
    GridModalComponent,
    HistoryComponent,
    ProductionChargerChartComponent,
    ProductionComponent,
    ProductionMeterChartComponent,
    ProductionModalComponent,
    ProductionSingleChartComponent,
    ProductionTotalAcChartComponent,
    ProductionTotalChartComponent,
    ProductionTotalDcChartComponent,
    SelfconsumptionChartComponent,
    SelfconsumptionModalComponent,
    SelfconsumptionWidgetComponent,
    SocStorageChartComponent,
    StorageChargerChartComponent,
    StorageComponent,
    StorageESSChartComponent,
    StorageModalComponent,
    StorageSingleChartComponent,
    StorageTotalChartComponent,
  ]
})
export class HistoryModule { }
