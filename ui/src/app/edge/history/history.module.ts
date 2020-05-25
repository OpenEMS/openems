import { AsymmetricPeakshavingChartComponent } from './peakshaving/asymmetric/chart.component';
import { AsymmetricPeakshavingModalComponent } from './peakshaving/asymmetric/modal/modal.component';
import { AsymmetricPeakshavingWidgetComponent } from './peakshaving/asymmetric/widget.component';
import { AutarchyChartComponent } from './autarchy/chart.component';
import { AutarchyModalComponent } from './autarchy/modal/modal.component';
import { AutarchyWidgetComponent } from './autarchy/widget.component';
import { ChannelthresholdModalComponent } from './channelthreshold/modal/modal.component';
import { ChannelthresholdSingleChartComponent } from './channelthreshold/singlechart.component';
import { ChannelthresholdTotalChartComponent } from './channelthreshold/totalchart.component';
import { ChanneltresholdWidgetComponent } from './channelthreshold/widget.component';
import { ConsumptionComponent } from './consumption/widget.component';
import { ConsumptionEvcsChartComponent } from './consumption/evcschart.component';
import { ConsumptionModalComponent } from './consumption/modal/modal.component';
import { ConsumptionOtherChartComponent } from './consumption/otherchart.component';
import { ConsumptionSingleChartComponent } from './consumption/singlechart.component';
import { ConsumptionTotalChartComponent } from './consumption/totalchart.component';
import { EnergyComponent } from './energy/energy.component';
import { EnergyModalComponent } from './energy/modal/modal.component';
import { GridChartComponent } from './grid/chart.component';
import { GridComponent } from './grid/widget.component';
import { GridModalComponent } from './grid/modal/modal.component';
import { HeatingelementChartComponent } from './heatingelement/chart.component';
import { HeatingelementModalComponent } from './heatingelement/modal/modal.component';
import { HeatingelementWidgetComponent } from './heatingelement/widget.component';
import { HistoryComponent } from './history.component';
import { NgModule } from '@angular/core';
import { ProductionChargerChartComponent } from './production/chargerchart.component';
import { ProductionComponent } from './production/widget.component';
import { ProductionMeterChartComponent } from './production/productionmeterchart';
import { ProductionModalComponent } from './production/modal/modal.component';
import { ProductionSingleChartComponent } from './production/singlechart';
import { ProductionTotalAcChartComponent } from './production/totalacchart';
import { ProductionTotalChartComponent } from './production/totalchart';
import { ProductionTotalDcChartComponent } from './production/totaldcchart';
import { SelfconsumptionChartComponent } from './selfconsumption/chart.component';
import { SelfconsumptionModalComponent } from './selfconsumption/modal/modal.component';
import { SelfconsumptionWidgetComponent } from './selfconsumption/widget.component';
import { SharedModule } from '../../shared/shared.module';
import { SinglethresholdChartComponent } from './singlethreshold/chart.component';
import { SinglethresholdModalComponent } from './singlethreshold/modal/modal.component';
import { SinglethresholdWidgetComponent } from './singlethreshold/widget.component';
import { SocStorageChartComponent } from './storage/socchart.component';
import { StorageChargerChartComponent } from './storage/chargerchart.component';
import { StorageComponent } from './storage/widget.component';
import { StorageESSChartComponent } from './storage/esschart.component';
import { StorageModalComponent } from './storage/modal/modal.component';
import { StorageSingleChartComponent } from './storage/singlechart.component';
import { StorageTotalChartComponent } from './storage/totalchart.component';
import { SymmetricPeakshavingChartComponent } from './peakshaving/symmetric/chart.component';
import { SymmetricPeakshavingModalComponent } from './peakshaving/symmetric/modal/modal.component';
import { SymmetricPeakshavingWidgetComponent } from './peakshaving/symmetric/widget.component';
import { ChpSocWidgetComponent } from './chpsoc/widget.component';
import { ChpSocModalComponent } from './chpsoc/modal/modal.component';
import { ChpSocChartComponent } from './chpsoc/chart.component';
import { ConsumptionMeterChartComponent } from './consumption/meterchart.component';

@NgModule({
  imports: [
    SharedModule,
  ],
  entryComponents: [
    AsymmetricPeakshavingModalComponent,
    AutarchyModalComponent,
    ChannelthresholdModalComponent,
    ChpSocModalComponent,
    ConsumptionModalComponent,
    EnergyModalComponent,
    GridModalComponent,
    HeatingelementModalComponent,
    ProductionModalComponent,
    SelfconsumptionModalComponent,
    SinglethresholdModalComponent,
    StorageModalComponent,
    SymmetricPeakshavingModalComponent,
  ],
  declarations: [
    AsymmetricPeakshavingChartComponent,
    AsymmetricPeakshavingModalComponent,
    AsymmetricPeakshavingWidgetComponent,
    AutarchyChartComponent,
    AutarchyModalComponent,
    AutarchyWidgetComponent,
    ChannelthresholdModalComponent,
    ChannelthresholdSingleChartComponent,
    ChannelthresholdTotalChartComponent,
    ChanneltresholdWidgetComponent,
    ChpSocChartComponent,
    ChpSocModalComponent,
    ChpSocWidgetComponent,
    ConsumptionComponent,
    ConsumptionEvcsChartComponent,
    ConsumptionMeterChartComponent,
    ConsumptionModalComponent,
    ConsumptionOtherChartComponent,
    ConsumptionSingleChartComponent,
    ConsumptionTotalChartComponent,
    EnergyComponent,
    EnergyModalComponent,
    GridChartComponent,
    GridComponent,
    GridModalComponent,
    HeatingelementChartComponent,
    HeatingelementModalComponent,
    HeatingelementWidgetComponent,
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
    SinglethresholdChartComponent,
    SinglethresholdModalComponent,
    SinglethresholdWidgetComponent,
    SocStorageChartComponent,
    StorageChargerChartComponent,
    StorageComponent,
    StorageESSChartComponent,
    StorageModalComponent,
    StorageSingleChartComponent,
    StorageTotalChartComponent,
    SymmetricPeakshavingChartComponent,
    SymmetricPeakshavingModalComponent,
    SymmetricPeakshavingWidgetComponent,
  ]
})
export class HistoryModule { }
