import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { differenceInDays } from 'date-fns';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { QueryHistoricTimeseriesDataResponse } from '../../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { Data, TooltipItem, Unit } from '../shared';

type ChartLabels = {
  production: string,
  discharge: string,
  consumption: string,
  price: string
}

@Component({
  selector: 'thermometerChart',
  templateUrl: '../abstracthistorychart.html'
})
export class ThermometerChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

  @Input() public period: DefaultTypes.HistoryPeriod;
  public sensors: EdgeConfig.Component[] = null;

  ngOnChanges() {
    this.updateChart();
  };

  constructor(
    protected service: Service,
    protected translate: TranslateService,
    private route: ActivatedRoute,
  ) {
    super("thermometer-chart", service, translate);
  }

  ngOnInit() {
    this.startSpinner();
    this.service.setCurrentComponent('', this.route);
  }

  ngOnDestroy() {
    this.unsubscribeChartRefresh()
  }

  protected updateChart() {
    this.autoSubscribeChartRefresh();
    this.startSpinner();
    this.colors = [];
    this.loading = true;

    this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
      this.service.getConfig().then(config => {
        let result = (response as QueryHistoricTimeseriesDataResponse).result;

        // convert labels
        let labels: Date[] = [];
        for (let timestamp of result.timestamps) {
          // Only use full hours as a timestamp
          labels.push(new Date(timestamp));
        }
        this.labels = labels;

        // convert datasets
        let datasets = [];

        let index: number = 0;

        let redColor = 255;
        let greenColor = 255;
        let blueColor = 255;

        this.sensors.forEach(sensor => {


          if ((sensor.id + '/Temperature') in result.data) {
            /*
            * Sensor
            */
            let sensorData = result.data[sensor.id + '/Temperature'].map(value => {
              if (value == null) {
                return null
              } else {
                return value / 10; // convert to °C
              }
            });

            // Coloring copied from consumption 'totalchart.component.ts'
            if (index == 0) {
              redColor = 0;
              greenColor = 153;
              blueColor = 153;
            } else if (index == this.sensors.length - 1) {
              redColor = 204;
              greenColor = 0;
              blueColor = 0;
            } else {
              redColor = (index + 1) % 3 == 0 ? 255 : 204 / (index + 1);
              greenColor = 0;
              blueColor = (index + 1) % 3 == 0 ? 127 : 408 / (index + 1);
            }
            index += 1;


            datasets.push({
              label: sensor.alias,
              data: sensorData,
              yAxisID: 'yAxis1',
              position: 'left'
            });
            this.colors.push({
              backgroundColor: 'rgba(' + redColor.toString() + ',' + greenColor.toString() + ',' + blueColor.toString() + ',0.05)',
              borderColor: 'rgba(' + redColor.toString() + ',' + greenColor.toString() + ',' + blueColor.toString() + ',1)'
            })

            index += 1;
          }

        })


        this.datasets = datasets;
        this.loading = false;
        this.stopSpinner();

      }).catch(reason => {
        console.error(reason); // TODO error message
        this.initializeChart();
        return;
      });

    }).catch(reason => {
      console.error(reason); // TODO error message
      this.initializeChart();
      return;
    });
  }

  protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
    return new Promise((resolve) => {
      let result: ChannelAddress[] = [];

      // Get sensorComponents
      this.sensors = config.getComponentsImplementingNature('io.openems.edge.thermometer.api.Thermometer')
        .filter(component => component.isEnabled)
        .sort((c1, c2) => c1.alias.localeCompare(c2.alias));

      for (let sensor of this.sensors) {
        result.push(
          new ChannelAddress(sensor.id, 'Temperature')
        )
      }
      resolve(result);
    });
  }

  protected setLabel(config: EdgeConfig) {
    let options = this.createDefaultChartOptions();
    let translate = this.translate;

    options.layout = {
      padding: {
        left: 2,
        right: 2,
        top: 0,
        bottom: 0
      }
    }

    options.scales.xAxes[0].stacked = true;

    //x-axis
    if (differenceInDays(this.service.historyPeriod.to, this.service.historyPeriod.from) >= 5) {
      options.scales.xAxes[0].time.unit = "day";
    } else {
      options.scales.xAxes[0].time.unit = "hour";
    }

    //y-axis
    options.scales.yAxes[0].id = "yAxis1"
    options.scales.yAxes[0].scaleLabel.labelString = "°C";
    options.scales.yAxes[0].scaleLabel.padding = -2;
    options.scales.yAxes[0].scaleLabel.fontSize = 11;
    options.scales.yAxes[0].ticks.padding = -5;
    options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
      let label = data.datasets[tooltipItem.datasetIndex].label;
      if (label.split(" ").length > 1) {
        label = label.split(" ").slice(0, 1).toString();
      }

      let value = tooltipItem.yLabel;
      return label + ": " + formatNumber(value, 'de', '1.0-2') + " °C";
    }
    this.options = options;
  }

  public getChartHeight(): number {
    return window.innerHeight / 1.3;
  }
}