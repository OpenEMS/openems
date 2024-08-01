import { Component } from '@angular/core';
import { AbstractHistoryChartOverview } from 'src/app/shared/components/chart/abstractHistoryChartOverview';

@Component({
  templateUrl: './currentVoltage.overview.html',
})
export class CurrentAndVoltageOverviewComponent extends AbstractHistoryChartOverview { }
