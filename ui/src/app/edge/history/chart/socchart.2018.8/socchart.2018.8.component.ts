import { Component, Input, OnInit, OnChanges, OnDestroy, ViewChild, AfterViewInit, SimpleChanges } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { BaseChartDirective } from 'ng2-charts/ng2-charts';
import { TranslateService } from '@ngx-translate/core';

import { Edge } from '../../../../shared/edge/edge';
import { DefaultTypes } from '../../../../shared/service/defaulttypes';
import { Dataset, EMPTY_DATASET } from './../../../../shared/shared';
import { DEFAULT_TIME_CHART_OPTIONS, ChartOptions } from './../shared';
import { Utils } from './../../../../shared/service/utils';

@Component({
  selector: 'socchart-2018-8',
  templateUrl: './socchart.2018.8.component.html'
})
export class SocChartComponent_2018_8 implements OnInit, OnChanges {

  @Input()
  set edge(edge: Edge) {
    this.stopOnDestroy.next();
    this._edge = edge;
    if (this._edge)
      this._edge.config.pipe(takeUntil(this.stopOnDestroy)).subscribe(config => {
        this.config = config;
        this.createChart();
      });
  }
  get edge(): Edge {
    return this._edge;
  }
  @Input() private channels: DefaultTypes.ChannelAddresses;
  @Input() private fromDate: Date;
  @Input() private toDate: Date;

  @ViewChild('socChart') private chart: BaseChartDirective;

  constructor(
    private utils: Utils,
    private translate: TranslateService
  ) {
  }

  public labels: Date[] = [];
  public datasets: Dataset[] = EMPTY_DATASET;
  public loading: boolean = true;
  private config: DefaultTypes.Config;
  private stopOnDestroy: Subject<void> = new Subject<void>();
  public _edge: Edge;


  private colors = [{
    backgroundColor: 'rgba(0,152,70,0.05)',
    borderColor: 'rgba(0,152,70,1)',
  }, {
    backgroundColor: 'rgba(0,152,204,0.05)',
    borderColor: 'rgba(0,152,204,1)'
  }, {
    backgroundColor: 'rgba(107,207,0,0.05)',
    borderColor: 'rgba(107,207,0,1)'
  }, {
    backgroundColor: 'rgba(224,232,17,0.05)',
    borderColor: 'rgba(224,232,17,1)'
  }
  ];
  private options: ChartOptions;

  ngOnInit() {
    let options = <ChartOptions>this.utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
    options.scales.yAxes[0].scaleLabel.labelString = this.translate.instant('General.Percentage');
    options.scales.yAxes[0].ticks.max = 100;
    this.options = options;
  }

  ngOnChanges() {
    this.createChart();
  };

  createChart() {
    if (this.channels && this.fromDate && this.toDate && this._edge) {
      if (Object.keys(this.channels).length === 0) {
        this.loading = true;
        return;
      }
      this.loading = true;
      // TODO stop previous subscribe; show only results for latest query. Otherwise the chart misbehaves on fast switch of period
      this._edge.historicDataQuery(this.fromDate, this.toDate, this.channels).then(historicData => {
        // prepare datas array and prefill with each device
        let tmpData: {
          [componentId: string]: number[];
        } = {};
        let labels: Date[] = [];
        for (let componentId in this.channels) {
          tmpData[componentId] = [];
        }
        for (let record of historicData.data) {
          // read timestamp and soc of each device
          labels.push(new Date(record.time));
          for (let componentId in this.channels) {
            let soc = null;
            if (componentId == '_sum' && "EssSoc" in record.channels[componentId]
              && record.channels[componentId]["EssSoc"] != null) {
              soc = Math.round(record.channels[componentId].EssSoc);
            }
            if (componentId in record.channels
              && "Soc" in record.channels[componentId]
              && record.channels[componentId]["Soc"] != null) {
              soc = Math.round(record.channels[componentId].Soc);
            }
            if (soc > 100 || soc < 0) {
              soc = null;
            }
            tmpData[componentId].push(soc);
          }
        }
        // refresh global datasets and labels
        let datasets = [];
        for (let componentId in tmpData) {
          datasets.push({
            label: this.translate.instant('General.Soc'),
            data: tmpData[componentId]
          });
        }
        this.datasets = datasets;
        this.labels = labels;
        // stop loading spinner
        this.loading = false;
        setTimeout(() => {
          // Workaround, because otherwise chart data and labels are not refreshed...
          if (this.chart) {
            this.chart.ngOnChanges({} as SimpleChanges);
          }
        });
      }).catch(error => {
        this.datasets = EMPTY_DATASET;
        this.labels = [];
        // stop loading spinner
        this.loading = false;
        // TODO error message
      });
    }
  }
}