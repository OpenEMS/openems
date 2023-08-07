import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { QueryHistoricTimeseriesEnergyResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { ChartAxis, HistoryUtils, YAxisTitle } from 'src/app/shared/service/utils';
import { ChannelAddress, EdgeConfig, Utils } from 'src/app/shared/shared';

@Component({
  selector: 'consumptionchart',
  templateUrl: '../../../../../shared/genericComponents/chart/abstracthistorychart.html'
})
export class ChartComponent extends AbstractHistoryChart {

  protected override getChartData() {
    return ChartComponent.getChartData(this.spinnerId, this.config, this.translate, this.showPhases, this.phaseColors);
  }

  public static getChartData(spinnerId: string, config: EdgeConfig, translate: TranslateService, showPhases: boolean, phaseColors: string[]): HistoryUtils.ChartData {
    spinnerId = "consumption";

    let inputChannel: HistoryUtils.InputChannel[] = [{
      name: 'ConsumptionActivePower',
      powerChannel: ChannelAddress.fromString('_sum/ConsumptionActivePower'),
      energyChannel: ChannelAddress.fromString('_sum/ConsumptionActiveEnergy')
    }
    ];

    ['L1', 'L2', 'L3'].forEach(phase => {
      inputChannel.push({
        name: 'ConsumptionActivePower' + phase,
        powerChannel: ChannelAddress.fromString('_sum/ConsumptionActivePower' + phase),
        energyChannel: ChannelAddress.fromString('_sum/ConsumptionActiveEnergy' + phase)
      });
    });

    let evcsComponents: EdgeConfig.Component[] = config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
      .filter(component => !(
        component.factoryId == 'Evcs.Cluster' ||
        component.factoryId == 'Evcs.Cluster.PeakShaving' ||
        component.factoryId == 'Evcs.Cluster.SelfConsumption'));

    let consumptionMeters: EdgeConfig.Component[] = config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
      .filter(component => component.isEnabled && config.isTypeConsumptionMetered(component));

    evcsComponents.forEach(component => {
      inputChannel.push({
        name: component.id + '/ChargePower',
        powerChannel: ChannelAddress.fromString(component.id + '/ChargePower'),
        energyChannel: ChannelAddress.fromString(component.id + '/ActiveConsumptionEnergy')
      });
    });
    consumptionMeters.forEach(meter => {
      inputChannel.push({
        name: meter.id + '/ActivePower',
        powerChannel: ChannelAddress.fromString(meter.id + '/ActivePower'),
        energyChannel: ChannelAddress.fromString(meter.id + '/ActiveConsumptionEnergy')
      });

      if (config.getNatureIdsByFactoryId(meter.factoryId).includes("io.openems.edge.meter.api.AsymmetricMeter")) {
        ['L1', 'L2', 'L3'].forEach(phase => {
          inputChannel.push({
            name: meter.id + '/ActivePower' + phase,
            powerChannel: ChannelAddress.fromString(meter.id + '/ActivePower' + phase),
            energyChannel: ChannelAddress.fromString(meter.id + '/ActiveConsumptionEnergy' + phase)
          });
        });
      }
    });

    return {
      input:
        [
          ...inputChannel
        ],
      output: (data: HistoryUtils.ChannelData) => {
        let datasets: HistoryUtils.DisplayValues[] = [];
        datasets.push({
          name: translate.instant('General.TOTAL'),
          nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
            return energyValues?.result.data['_sum/ConsumptionActiveEnergy'];
          },
          converter: () => {
            return data['ConsumptionActivePower'];
          },
          color: 'rgb(253,197,7)',
          stack: 0,
          hiddenOnInit: true,
          noStrokeThroughLegendIfHidden: false
        });

        if (showPhases) {
          ['L1', 'L2', 'L3'].forEach((phase, index) => {
            datasets.push({
              name: translate.instant('General.phase') + " " + phase,
              nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
                return energyValues?.result.data['_sum/ConsumptionActiveEnergy' + phase];
              },
              converter: () => {
                console.log("data", data);
                return data['ConsumptionActivePower' + phase];
              },
              color: phaseColors[Math.min(index, phaseColors.length - 1)],
              stack: 1
            });
          });
        }
        let evcsComponentColors: string[] = ['rgb(0,223,0)', 'rgb(0,178,0)', 'rgb(0,201,0)', 'rgb(0,134,0)', 'rgb(0,156,0)'];
        evcsComponents.forEach((component, index) => {
          datasets.push({
            name: component.alias,
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return energyValues?.result.data[component.id + '/ActiveConsumptionEnergy'];
            },
            converter: () => {
              return data[component.id + '/ChargePower'] ?? null;
            },
            color: evcsComponentColors[Math.min(index, (evcsComponentColors.length - 1))],
            stack: 1
          });
        });

        let consumptionMeterColors: string[] = ['rgb(220,20,60)', 'rgb(202, 158, 6', 'rgb(228, 177, 6)', 'rgb(177, 138, 5)', 'rgb(152, 118, 4)'];
        consumptionMeters.forEach((meter, index) => {
          datasets.push({
            name: meter.alias,
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return energyValues?.result.data[meter.id + '/ActiveConsumptionEnergy'];
            },
            converter: () => {
              return data[meter.id + '/ActivePower'] ?? null;
            },
            color: consumptionMeterColors[Math.min(index, (consumptionMeterColors.length - 1))],
            stack: 2
          });

          if (showPhases) {
            ['L1', 'L2', 'L3'].forEach((phase, index) => {
              datasets.push({
                name: meter.alias + " " + translate.instant('General.phase') + " " + phase,
                nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
                  return energyValues?.result.data[meter.id + '/ActiveConsumptionEnergy' + phase];
                },
                converter: () => {
                  return data[meter.id + '/ActivePower' + phase];
                },
                color: phaseColors[index],
                stack: 2
              });
            });
          }
        });

        // other consumption
        if (consumptionMeters.length > 0 || evcsComponents.length > 0) {
          datasets.push({
            name: translate.instant('General.otherConsumption'),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return Utils.calculateOtherConsumptionTotal(energyValues, evcsComponents, consumptionMeters);
            },
            converter: () => {
              return Utils.calculateOtherConsumption(data, evcsComponents, consumptionMeters);
            },
            color: 'rgb(0,223,0)',
            stack: 3
          });
        }

        return datasets;
      },
      tooltip: {
        formatNumber: '1.0-2'
      },
      yAxes: [
        {
          unit: YAxisTitle.ENERGY,
          position: 'left',
          yAxisId: ChartAxis.LEFT
        }]
    };
  }
}