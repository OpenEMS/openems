import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnInit, Output, EventEmitter } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Service, Utils, EdgeConfig } from 'src/app/shared/shared';
import { ChartOptions, ChartType, ChartDataSets } from 'chart.js';
import { Label } from 'ng2-charts';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'correntlyChart',
  templateUrl: './chart.component.html'
})
export class CorrentlyChartComponent {

  @Output() startDateForecast = new EventEmitter<any>();
  @Output() endDateForecast = new EventEmitter<any>();
  @Output() zipCode = new EventEmitter<any>();
  @Output() city = new EventEmitter<any>();


  public barChartOptions: ChartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    // We use these empty structures as placeholders for dynamic theming.
    scales: {
      xAxes: [{
        scaleLabel: {
          display: true,
          labelString: 'Uhrzeit'
        },
      }], yAxes: [
        {
          id: 'yAxis1',
          position: 'left',
          scaleLabel: {
            display: true,
            labelString: 'GSI'
          },
        },
        {
          id: 'yAxis2',
          position: 'right',
          scaleLabel: {
            display: true,
            labelString: 'Cent/kWh'
          },
        }
      ]
    },
    plugins: {
      datalabels: {
        anchor: 'end',
        align: 'end',
      }
    },
    legend: {
      position: 'bottom'
    }
  };

  constructor(
    protected service: Service,
    private route: ActivatedRoute,
    private translate: TranslateService,
    private httpClient: HttpClient,
  ) { }

  public barChartLabels: Label[] = [];
  public barChartType: ChartType = 'bar';
  public barChartLegend = true;

  public apiCallSuccessful: boolean = false;


  public barChartData: ChartDataSets[] = [
    { data: [], label: 'Cent/kWh', backgroundColor: [], borderColor: [], hoverBackgroundColor: [], yAxisID: "yAxis2", stack: 'a' },
    { data: [], label: 'GSI', backgroundColor: [], borderColor: [], hoverBackgroundColor: [], yAxisID: "yAxis1", stack: 'a' },
  ];

  ngOnInit() {
    this.service.setCurrentComponent('', this.route);
    this.service.getConfig().then(config => {
      this.setChart(config);

    })
  }

  setChart(config: EdgeConfig) {
    let plz = config.getComponent("corrently0").properties["zipCode"]
    this.httpClient.get("https://api.corrently.io/core/gsi?plz=" + plz).subscribe((res) => {
      this.startDateForecast.emit(new Date(res['forecast'][0]['timeStamp']).toLocaleString('de-DE', { year: 'numeric', month: '2-digit', day: '2-digit' }));
      this.endDateForecast.emit(new Date(res['forecast'][res['forecast'].length - 1]['timeStamp']).toLocaleString('de-DE', { month: '2-digit', day: '2-digit' }))
      this.zipCode.emit(res["location"]["zip"])
      this.city.emit(res["location"]["city"])
      res["forecast"].forEach((object, index) => {
        let label;
        if (new Date(object['timeStamp']).getHours().toString().length == 2) {
          label = new Date(object['timeStamp']).toLocaleString('de-DE', { month: '2-digit', day: '2-digit' }).toString() + "\n" +
            new Date(object['timeStamp']).getHours().toString() + ":0" + new Date(object['timeStamp']).getMinutes().toString();
        } else if (new Date(object['timeStamp']).getHours().toString().length == 1) {
          label = new Date(object['timeStamp']).toLocaleString('de-DE', { month: '2-digit', day: '2-digit' }).toString() + "\n" +
            "0" + new Date(object['timeStamp']).getHours().toString() + ":0" + new Date(object['timeStamp']).getMinutes().toString();
        }

        if (object["gsi"] >= 0) {
          this.barChartData[1].backgroundColor[index] = 'rgba(255,53,0, 1)'
          this.barChartData[1].borderColor[index] = 'rgba(255,53,0, 1)'
          this.barChartData[1].hoverBackgroundColor[index] = 'rgba(255,53,0, 1)'
        }
        if (object["gsi"] > 41) {
          this.barChartData[1].backgroundColor[index] = 'rgba(255,255,0, 1)'
          this.barChartData[1].borderColor[index] = 'rgba(255,53,0, 1)'
          this.barChartData[1].hoverBackgroundColor[index] = 'rgba(255,255,0, 1)'
        }
        if (object["gsi"] > 60) {
          this.barChartData[1].backgroundColor[index] = 'rgba(0,223,0, 1)'
          this.barChartData[1].borderColor[index] = 'rgba(255,53,0, 1)'
          this.barChartData[1].hoverBackgroundColor[index] = 'rgba(0,223,0, 1)'
        }
        this.barChartData[0].backgroundColor[index] = 'rgba(255,255,255,0.5)';
        this.barChartData[0].borderColor[index] = 'rgba(255,255,255,0.5)';
        this.barChartData[0].hoverBackgroundColor[index] = 'rgba(255,255,255,0.5)';

        let kwhPrice: number = 2 - (2 / 100 * object["gsi"])
        this.barChartData[0].data.push(Math.round(kwhPrice * 100) / 100)
        this.barChartLabels.push(label);
        this.barChartData[1].data.push(object["gsi"])
      });
      this.apiCallSuccessful = true;
    })
  }

  public getChartHeight(): number {
    return window.innerHeight / 21 * 9;
  }
}