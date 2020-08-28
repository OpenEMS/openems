import { AsymmetricPeakshavingChartComponent } from './peakshaving/asymmetric/chart.component';
import { AsymmetricPeakshavingWidgetComponent } from './peakshaving/asymmetric/widget.component';
import { AutarchyChartComponent } from './autarchy/chart.component';
import { AutarchyChartOverviewComponent } from './autarchy/autarchychartoverview/autarchychartoverview.component';
import { AutarchyWidgetComponent } from './autarchy/widget.component';
import { ChannelthresholdChartOverviewComponent } from './channelthreshold/channelthresholdchartoverview/channelthresholdchartoverview.component';
import { ChannelthresholdSingleChartComponent } from './channelthreshold/singlechart.component';
import { ChannelthresholdTotalChartComponent } from './channelthreshold/totalchart.component';
import { ChanneltresholdWidgetComponent } from './channelthreshold/widget.component';
import { ChpSocChartComponent } from './chpsoc/chart.component';
import { ChpSocWidgetComponent } from './chpsoc/widget.component';
import { ConsumptionChartOverviewComponent } from './consumption/consumptionchartoverview/consumptionchartoverview.component';
import { ConsumptionComponent } from './consumption/widget.component';
import { ConsumptionEvcsChartComponent } from './consumption/evcschart.component';
import { ConsumptionMeterChartComponent } from './consumption/meterchart.component';
import { ConsumptionOtherChartComponent } from './consumption/otherchart.component';
import { ConsumptionSingleChartComponent } from './consumption/singlechart.component';
import { ConsumptionTotalChartComponent } from './consumption/totalchart.component';
import { EnergyComponent } from './energy/energy.component';
import { EnergyModalComponent } from './energy/modal/modal.component';
import { FixDigitalOutputChartOverviewComponent } from './fixdigitaloutput/fixdigitaloutputchartoverview/fixdigitaloutputchartoverview.component';
import { FixDigitalOutputSingleChartComponent } from './fixdigitaloutput/singlechart.component';
import { FixDigitalOutputTotalChartComponent } from './fixdigitaloutput/totalchart.component';
import { FixDigitalOutputWidgetComponent } from './fixdigitaloutput/widget.component';
import { GridChartComponent } from './grid/chart.component';
import { GridChartOverviewComponent } from './grid/gridchartoverview/gridchartoverview.component';
import { GridComponent } from './grid/widget.component';
import { HeatingelementChartComponent } from './heatingelement/chart.component';
import { HeatingelementChartOverviewComponent } from './heatingelement/heatingelementchartoverview/heatingelementchartoverview.component';
import { HeatingelementWidgetComponent } from './heatingelement/widget.component';
import { HistoryComponent } from './history.component';
import { NgModule } from '@angular/core';
import { ProductionChargerChartComponent } from './production/chargerchart.component';
import { ProductionChartOverviewComponent } from './production/productionchartoverview/productionchartoverview.component';
import { ProductionComponent } from './production/widget.component';
import { ProductionMeterChartComponent } from './production/productionmeterchart';
import { ProductionSingleChartComponent } from './production/singlechart';
import { ProductionTotalAcChartComponent } from './production/totalacchart';
import { ProductionTotalChartComponent } from './production/totalchart';
import { ProductionTotalDcChartComponent } from './production/totaldcchart';
import { SelfconsumptionChartComponent } from './selfconsumption/chart.component';
import { SelfconsumptionChartOverviewComponent } from './selfconsumption/selfconsumptionchartoverview/selfconsumptionchartoverview.component';
import { SelfconsumptionWidgetComponent } from './selfconsumption/widget.component';
import { SharedModule } from '../../shared/shared.module';
import { SinglethresholdChartComponent } from './singlethreshold/chart.component';
import { SinglethresholdChartOverviewComponent } from './singlethreshold/singlethresholdchartoverview/singlethresholdchartoverview.component';
import { SinglethresholdWidgetComponent } from './singlethreshold/widget.component';
import { SocStorageChartComponent } from './storage/socchart.component';
import { StorageChargerChartComponent } from './storage/chargerchart.component';
import { StorageChartOverviewComponent } from './storage/storagechartoverview/storagechartoverview.component';
import { StorageComponent } from './storage/widget.component';
import { StorageESSChartComponent } from './storage/esschart.component';
import { StorageSingleChartComponent } from './storage/singlechart.component';
import { StorageTotalChartComponent } from './storage/totalchart.component';
import { SymmetricPeakshavingChartComponent } from './peakshaving/symmetric/chart.component';
import { SymmetricPeakshavingChartoOverviewComponent } from './peakshaving/symmetric/symmetricpeakshavingchartoverview/symmetricpeakshavingchart.component';
import { SymmetricPeakshavingWidgetComponent } from './peakshaving/symmetric/widget.component';

@NgModule({
  imports: [
    SharedModule,
  ],
  entryComponents: [
    EnergyModalComponent,
  ],
  declarations: [
    AsymmetricPeakshavingChartComponent,
    AsymmetricPeakshavingChartComponent,
    AsymmetricPeakshavingWidgetComponent,
    AutarchyChartComponent,
    AutarchyChartOverviewComponent,
    AutarchyWidgetComponent,
    ChannelthresholdChartOverviewComponent,
    ChannelthresholdSingleChartComponent,
    ChannelthresholdTotalChartComponent,
    ChanneltresholdWidgetComponent,
    ChpSocChartComponent,
    ChpSocWidgetComponent,
    ConsumptionChartOverviewComponent,
    ConsumptionComponent,
    ConsumptionEvcsChartComponent,
    ConsumptionMeterChartComponent,
    ConsumptionOtherChartComponent,
    ConsumptionSingleChartComponent,
    ConsumptionTotalChartComponent,
    EnergyComponent,
    EnergyModalComponent,
    FixDigitalOutputChartOverviewComponent,
    FixDigitalOutputSingleChartComponent,
    FixDigitalOutputTotalChartComponent,
    FixDigitalOutputWidgetComponent,
    GridChartComponent,
    GridChartOverviewComponent,
    GridComponent,
    HeatingelementChartComponent,
    HeatingelementChartOverviewComponent,
    HeatingelementWidgetComponent,
    HistoryComponent,
    ProductionChargerChartComponent,
    ProductionChartOverviewComponent,
    ProductionComponent,
    ProductionMeterChartComponent,
    ProductionSingleChartComponent,
    ProductionTotalAcChartComponent,
    ProductionTotalChartComponent,
    ProductionTotalDcChartComponent,
    SelfconsumptionChartComponent,
    SelfconsumptionChartOverviewComponent,
    SelfconsumptionWidgetComponent,
    SinglethresholdChartComponent,
    SinglethresholdChartOverviewComponent,
    SinglethresholdWidgetComponent,
    SocStorageChartComponent,
    StorageChargerChartComponent,
    StorageChartOverviewComponent,
    StorageComponent,
    StorageESSChartComponent,
    StorageSingleChartComponent,
    StorageTotalChartComponent,
    SymmetricPeakshavingChartComponent,
    SymmetricPeakshavingChartoOverviewComponent,
    SymmetricPeakshavingWidgetComponent,
  ]
})
export class HistoryModule { }
