import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/components/chart/abstracthistorychart';
import { Phase } from 'src/app/shared/components/shared/phase';
import { QueryHistoricTimeseriesEnergyResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { ChartAxis, HistoryUtils, YAxisTitle } from 'src/app/shared/service/utils';
import { ChannelAddress } from 'src/app/shared/shared';

@Component({
  selector: 'meterChart',
  templateUrl: '../../../../../../shared/components/chart/abstracthistorychart.html',
})
export class ChartComponent extends AbstractHistoryChart {


  protected override getChartData(): HistoryUtils.ChartData {

    const component = this.config.getComponent(this.route.snapshot.params.componentId);
    const isProductionMeter = this.config.hasComponentNature("io.openems.edge.meter.api.ElectricityMeter", component.id) && this.config.isProducer(component);
    const isCharger = this.config.hasComponentNature("io.openems.edge.ess.dccharger.api.EssDcCharger", component.id);

    const channels: HistoryUtils.InputChannel[] = [];

    if (isCharger) {
      channels.push({
        name: component.id,
        powerChannel: ChannelAddress.fromString(component.id + '/ActualPower'),
        energyChannel: ChannelAddress.fromString(component.id + '/ActualEnergy'),
      });
    }

    if (isProductionMeter) {
      channels.push({
        name: component.id,
        powerChannel: ChannelAddress.fromString(component.id + '/ActivePower'),
        energyChannel: ChannelAddress.fromString(component.id + '/ActiveProductionEnergy'),
      });

      channels.push(...Phase.THREE_PHASE.map(phase => ({
        name: 'ProductionAcActivePower' + phase,
        powerChannel: ChannelAddress.fromString(component.id + '/ActivePower' + phase),
      })));
    }

    const chartObject: HistoryUtils.ChartData = {
      input: channels,
      output: (data: HistoryUtils.ChannelData) => {
        const datasets: HistoryUtils.DisplayValue[] = [];
        datasets.push({
          name: component.alias,
          nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => {
            return energyQueryResponse.result.data[isCharger ? component.id + '/ActualEnergy' : component.id + '/ActiveProductionEnergy'];
          },
          converter: () => {
            return data[component.id];
          },
          color: 'rgb(0,152,204)',
          hiddenOnInit: false,
          stack: 2,
        });

        if (!isProductionMeter) {
          return datasets;
        }

        datasets.push(...Phase.THREE_PHASE.map((phase, i) => ({
          name: "Phase " + phase,
          converter: () =>
            data['ProductionAcActivePower' + phase],
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
