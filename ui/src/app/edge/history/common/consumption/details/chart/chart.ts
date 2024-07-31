import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/components/chart/abstracthistorychart';
import { Phase } from 'src/app/shared/components/shared/phase';
import { QueryHistoricTimeseriesEnergyResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { ChartAxis, HistoryUtils, YAxisTitle } from 'src/app/shared/service/utils';
import { ChannelAddress } from 'src/app/shared/shared';

@Component({
  selector: 'consumptionMeterChart',
  templateUrl: '../../../../../../shared/components/chart/abstracthistorychart.html',
})
export class ChartComponent extends AbstractHistoryChart {

  protected override getChartData(): HistoryUtils.ChartData {

    const component = this.config?.getComponent(this.route.snapshot.params.componentId);

    const isConsumptionMetered: boolean = this.config?.hasComponentNature("io.openems.edge.meter.api.ElectricityMeter", component?.id)
      && this.config?.isTypeConsumptionMetered(component);
    const isEvcs: boolean = this.config?.hasComponentNature("io.openems.edge.evcs.api.Evcs", component?.id)
      && (component?.factoryId !== 'Evcs.Cluster.SelfConsumption')
      && component?.factoryId !== 'Evcs.Cluster.PeakShaving'
      && component?.isEnabled !== false;
    const channels: HistoryUtils.InputChannel[] = [];

    if (isEvcs) {
      channels.push({
        name: component.id,
        powerChannel: ChannelAddress.fromString(component.id + '/ChargePower'),
        energyChannel: ChannelAddress.fromString(component.id + '/ActiveConsumptionEnergy'),
      });
    }

    if (isConsumptionMetered) {
      channels.push({
        name: component.id,
        powerChannel: ChannelAddress.fromString(component.id + '/ActivePower'),
        energyChannel: ChannelAddress.fromString(component.id + '/ActiveProductionEnergy'),
      });

      channels.push(...Phase.THREE_PHASE.map(phase => ({
        name: 'ConsumptionActivePower' + phase,
        powerChannel: ChannelAddress.fromString(component.id + '/ActivePower' + phase),
        energyChannel: ChannelAddress.fromString(component.id + '/ActiveProductionEnergy' + phase),
      })));
    }

    const chartObject: HistoryUtils.ChartData = {
      input: channels,
      output: (data: HistoryUtils.ChannelData) => {
        const datasets: HistoryUtils.DisplayValues[] = [];
        datasets.push({
          name: component.alias,
          nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => {
            return energyQueryResponse.result.data[isEvcs ? component.id + '/ActiveConsumptionEnergy' : component.id + '/ActiveProductionEnergy'];
          },
          converter: () => {
            return data[component.id];
          },
          color: 'rgb(0,152,204)',
          hiddenOnInit: false,
          stack: 2,
        });

        if (!isConsumptionMetered) {
          return datasets;
        }

        datasets.push(...Phase.THREE_PHASE.map((phase, i) => ({
          name: "Phase " + phase,
          nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => energyQueryResponse.result.data[component.id + '/ActiveProductionEnergy' + phase],
          converter: () =>
            data['ConsumptionActivePower' + phase],
          color: 'rgb(' + AbstractHistoryChart.phaseColors[i] + ')',
          stack: 3,
        })));

        return datasets;
      },
      tooltip: {
        formatNumber: '1.1-2',
        afterTitle: this.translate.instant('General.TOTAL'),
      },
      yAxes: [{
        unit: YAxisTitle.ENERGY,
        position: 'left',
        yAxisId: ChartAxis.LEFT,
      }],
    };

    return chartObject;
  }
}
