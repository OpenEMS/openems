import { NgModule } from '@angular/core';

import { SharedModule } from '../../shared/shared.module';
import { ChannelthresholdChartOverviewComponent } from './channelthreshold/channelthresholdchartoverview/channelthresholdchartoverview.component';
import { ChannelthresholdSingleChartComponent } from './channelthreshold/singlechart.component';
import { ChannelthresholdTotalChartComponent } from './channelthreshold/totalchart.component';
import { ChannelthresholdWidgetComponent } from './channelthreshold/widget.component';
import { ChpSocChartComponent } from './chpsoc/chart.component';
import { ChpSocWidgetComponent } from './chpsoc/widget.component';
import { Common_Autarchy } from './common/autarchy/Autarchy';
import { Common_Production } from './common/production/Production';
import { DelayedSellToGridChartComponent } from './delayedselltogrid/chart.component';
import { DelayedSellToGridChartOverviewComponent } from './delayedselltogrid/symmetricpeakshavingchartoverview/delayedselltogridchartoverview.component';
import { DelayedSellToGridWidgetComponent } from './delayedselltogrid/widget.component';
import { EnergyComponent } from './energy/energy.component';
import { EnergyModalComponent } from './energy/modal/modal.component';
import { FixDigitalOutputChartOverviewComponent } from './fixdigitaloutput/fixdigitaloutputchartoverview/fixdigitaloutputchartoverview.component';
import { FixDigitalOutputSingleChartComponent } from './fixdigitaloutput/singlechart.component';
import { FixDigitalOutputTotalChartComponent } from './fixdigitaloutput/totalchart.component';
import { FixDigitalOutputWidgetComponent } from './fixdigitaloutput/widget.component';
import { GridChartComponent } from './grid/chart.component';
import { GridChartOverviewComponent } from './grid/gridchartoverview/gridchartoverview.component';
import { GridComponent } from './grid/widget.component';
import { GridOptimizedChargeChartComponent } from './gridoptimizedcharge/chart.component';
import { GridOptimizedChargeChartOverviewComponent } from './gridoptimizedcharge/gridoptimizedchargechartoverview/gridoptimizedchargechartoverview.component';
import { SellToGridLimitChartComponent } from './gridoptimizedcharge/sellToGridLimitChart.component';
import { GridOptimizedChargeWidgetComponent } from './gridoptimizedcharge/widget.component';
import { HeatingelementChartComponent } from './heatingelement/chart.component';
import { HeatingelementChartOverviewComponent } from './heatingelement/heatingelementchartoverview/heatingelementchartoverview.component';
import { HeatingelementWidgetComponent } from './heatingelement/widget.component';
import { HeatPumpChartComponent } from './heatpump/chart.component';
import { HeatPumpChartOverviewComponent } from './heatpump/heatpumpchartoverview/heatpumpchartoverview.component';
import { HeatpumpWidgetComponent } from './heatpump/widget.component';
import { HistoryComponent } from './history.component';
import { HistoryParentComponent } from './historyparent.component';
import { AsymmetricPeakshavingChartOverviewComponent } from './peakshaving/asymmetric/asymmetricpeakshavingchartoverview/asymmetricpeakshavingchartoverview.component';
import { AsymmetricPeakshavingChartComponent } from './peakshaving/asymmetric/chart.component';
import { AsymmetricPeakshavingWidgetComponent } from './peakshaving/asymmetric/widget.component';
import { SymmetricPeakshavingChartComponent } from './peakshaving/symmetric/chart.component';
import { SymmetricPeakshavingChartOverviewComponent } from './peakshaving/symmetric/symmetricpeakshavingchartoverview/symmetricpeakshavingchartoverview.component';
import { SymmetricPeakshavingWidgetComponent } from './peakshaving/symmetric/widget.component';
import { TimeslotPeakshavingChartComponent } from './peakshaving/timeslot/chart.component';
import { TimeslotPeakshavingChartOverviewComponent } from './peakshaving/timeslot/timeslotpeakshavingchartoverview/timeslotpeakshavingchartoverview.component';
import { TimeslotPeakshavingWidgetComponent } from './peakshaving/timeslot/widget.component';
import { SelfconsumptionChartComponent } from './selfconsumption/chart.component';
import { SelfconsumptionChartOverviewComponent } from './selfconsumption/selfconsumptionchartoverview/selfconsumptionchartoverview.component';
import { SelfconsumptionWidgetComponent } from './selfconsumption/widget.component';
import { SinglethresholdChartComponent } from './singlethreshold/chart.component';
import { SinglethresholdChartOverviewComponent } from './singlethreshold/singlethresholdchartoverview/singlethresholdchartoverview.component';
import { SinglethresholdWidgetComponent } from './singlethreshold/widget.component';
import { StorageChargerChartComponent } from './storage/chargerchart.component';
import { StorageESSChartComponent } from './storage/esschart.component';
import { StorageSingleChartComponent } from './storage/singlechart.component';
import { SocStorageChartComponent } from './storage/socchart.component';
import { StorageChartOverviewComponent } from './storage/storagechartoverview/storagechartoverview.component';
import { StorageTotalChartComponent } from './storage/totalchart.component';
import { StorageComponent } from './storage/widget.component';
import { TimeOfUseTariffDischargeChartComponent } from './timeofusetariffdischarge/chart.component';
import { TimeOfUseTariffDischargeChartOverviewComponent } from './timeofusetariffdischarge/timeofusetariffdischargeoverview/timeofusetariffdischargechartoverview.component';
import { TimeOfUseTariffDischargeWidgetComponent } from './timeofusetariffdischarge/widget.component';
import { Common_Consumption } from './common/consumption/Consumption';

@NgModule({
  imports: [
    SharedModule,
    Common_Autarchy,
    Common_Production,
    Common_Consumption
  ],
  entryComponents: [
    EnergyModalComponent,
  ],
  declarations: [
    AsymmetricPeakshavingChartComponent,
    AsymmetricPeakshavingChartOverviewComponent,
    AsymmetricPeakshavingWidgetComponent,
    ChannelthresholdChartOverviewComponent,
    ChannelthresholdSingleChartComponent,
    ChannelthresholdTotalChartComponent,
    ChannelthresholdWidgetComponent,
    ChpSocChartComponent,
    ChpSocWidgetComponent,
    DelayedSellToGridChartComponent,
    DelayedSellToGridChartOverviewComponent,
    DelayedSellToGridWidgetComponent,
    EnergyComponent,
    EnergyModalComponent,
    FixDigitalOutputChartOverviewComponent,
    FixDigitalOutputSingleChartComponent,
    FixDigitalOutputTotalChartComponent,
    FixDigitalOutputWidgetComponent,
    GridChartComponent,
    GridChartOverviewComponent,
    GridComponent,
    GridOptimizedChargeChartComponent,
    GridOptimizedChargeChartOverviewComponent,
    GridOptimizedChargeWidgetComponent,
    HeatingelementChartComponent,
    HeatingelementChartOverviewComponent,
    HeatingelementWidgetComponent,
    HeatPumpChartComponent,
    HeatPumpChartOverviewComponent,
    HeatpumpWidgetComponent,
    HistoryComponent,
    SelfconsumptionChartComponent,
    SelfconsumptionChartOverviewComponent,
    SelfconsumptionWidgetComponent,
    SellToGridLimitChartComponent,
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
    SymmetricPeakshavingChartOverviewComponent,
    SymmetricPeakshavingWidgetComponent,
    TimeOfUseTariffDischargeChartComponent,
    TimeOfUseTariffDischargeChartOverviewComponent,
    TimeOfUseTariffDischargeWidgetComponent,
    TimeslotPeakshavingChartComponent,
    TimeslotPeakshavingChartOverviewComponent,
    TimeslotPeakshavingWidgetComponent,
    HistoryParentComponent,
  ]
})
export class HistoryModule { }
