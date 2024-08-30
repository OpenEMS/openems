// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "chart.js";
import { DefaultTypes } from "src/app/shared/service/defaulttypes";
import { YAxisType } from "src/app/shared/service/utils";

import { QueryHistoricTimeseriesDataResponse } from "../../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { ChannelAddress, Edge, EdgeConfig, Service } from "../../../shared/shared";
import { AbstractHistoryChart } from "../abstracthistorychart";

@Component({
  selector: "fixDigitalOutputSingleChart",
  templateUrl: "../abstracthistorychart.html",
})
export class FixDigitalOutputSingleChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

  @Input({ required: true }) public period!: DefaultTypes.HistoryPeriod;
  @Input({ required: true }) public componentId!: string;


  constructor(
    protected override service: Service,
    protected override translate: TranslateService,
    private route: ActivatedRoute,
  ) {
    super("fixdigitaloutput-single-chart", service, translate);
  }

  ngOnChanges() {
    this.updateChart();
  }

  ngOnInit() {
    this.startSpinner();
    this.service.setCurrentComponent("", this.route);
  }

  ngOnDestroy() {
    this.unsubscribeChartRefresh();
  }

  public getChartHeight(): number {
    return window.innerHeight / 1.3;
  }

  protected updateChart() {
    this.autoSubscribeChartRefresh();
    this.startSpinner();
    this.colors = [];
    this.loading = true;
    this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
      const result = (response as QueryHistoricTimeseriesDataResponse).result;
      // convert labels
      const labels: Date[] = [];
      for (const timestamp of result.timestamps) {
        labels.push(new Date(timestamp));
      }
      this.labels = labels;

      // convert datasets
      const datasets: Chart.ChartDataset[] = [];
      for (const channel in result.data) {
        const address = ChannelAddress.fromString(channel);
        const data = result.data[channel].map(value => {
          if (value == null) {
            return null;
          } else {
            return value * 100; // convert to % [0,100]
          }
        });
        datasets.push({
          label: address.channelId,
          data: data,
        });
        this.colors.push({
          backgroundColor: "rgba(0,191,255,0.05)",
          borderColor: "rgba(0,191,255,1)",
        });
      }
      this.datasets = datasets;
      this.loading = false;
      this.stopSpinner();
    }).catch(reason => {
      console.error(reason); // TODO error message
      this.initializeChart();
      return;
    }).finally(async () => {
      this.unit = YAxisType.PERCENTAGE;
      await this.setOptions(this.options);
      this.stopSpinner();
    });
  }

  protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
    return new Promise((resolve) => {
      const outputChannel = ChannelAddress.fromString(config.getComponentProperties(this.componentId)["outputChannelAddress"]);
      const channeladdresses = [outputChannel];
      resolve(channeladdresses);
    });
  }

  protected setLabel() {
    this.options = this.createDefaultChartOptions();
  }

}
