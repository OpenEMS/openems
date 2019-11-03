import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { HistoryComponent } from './history.component';
import { EnergyComponent } from './energy/energy.component';
import { KwhComponent } from './kwh/kwh.component';
import { ChannelthresholdTotalChartComponent } from './channelthreshold/totalchart.component';
import { EvcsChartComponent } from './evcs/chart.component';
import { ChpSocTotalChartComponent } from './chpsoc/totalchart.component';
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
import { StorageSingleChartComponent } from './storage/singlechart.component';
import { GridChartComponent } from './grid/chart.component';
import { SocStorageChartComponent } from './storage/socchart.component';
import { EnergyModalComponent } from './energy/modal/modal.component';
import { StorageESSChartComponent } from './storage/esschart.component';
import { StorageChargerChartComponent } from './storage/chargerchart.component';
import { StorageTotalChartComponent } from './storage/totalchart.component';
import { ProductionTotalAcChartComponent } from './production/totalacchart';
import { ProductionTotalDcChartComponent } from './production/totaldcchart';
import { ProductionMeterChartComponent } from './production/productionmeterchart';
import { ProductionChargerChartComponent } from './production/chargerchart.component';
import { ProductionSingleChartComponent } from './production/singlechart';
import { ConsumptionTotalChartComponent } from './consumption/totalchart.component';
import { ConsumptionEvcsChartComponent } from './consumption/evcschart.component';
import { ConsumptionOtherChartComponent } from './consumption/otherchart.component';
import { ConsumptionSingleChartComponent } from './consumption/singlechart.component';
import { ProductionTotalChartComponent } from './production/totalchart';
import { ChannelthresholdSingleChartComponent } from './channelthreshold/singlechart.component';
import { ChpSocSingleChartComponent } from './chpsoc/singlechart.component';


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
    AutarchyChartComponent,
    ChpSocTotalChartComponent,
    ChpSocSingleChartComponent,
    ChannelthresholdTotalChartComponent,
    ChannelthresholdSingleChartComponent,
    ConsumptionComponent,
    EnergyComponent,
    EvcsChartComponent,
    EvcsWidgetComponent,
    GridComponent,
    HistoryComponent,
    KwhComponent,
    StorageComponent,
    ProductionComponent,
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
    StorageESSChartComponent,
    SelfconsumptionChartComponent,
    GridChartComponent,
    SocStorageChartComponent,
    EnergyModalComponent,
    StorageSingleChartComponent,
    StorageChargerChartComponent,
    StorageTotalChartComponent,
    ProductionSingleChartComponent,
    ProductionTotalAcChartComponent,
    ProductionTotalDcChartComponent,
    ProductionMeterChartComponent,
    ProductionChargerChartComponent,
    ProductionTotalChartComponent,
    ConsumptionTotalChartComponent,
    ConsumptionSingleChartComponent,
    ConsumptionEvcsChartComponent,
    ConsumptionOtherChartComponent
  ]
})
export class HistoryModule { }
