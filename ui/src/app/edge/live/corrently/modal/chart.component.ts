import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
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

  public barChartColors = []


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
          position: 'left',
          scaleLabel: {
            display: true,
            labelString: 'GSI'
          },
        },
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
    { data: [], label: 'GSI', backgroundColor: [], hoverBackgroundColor: [] },
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
          this.barChartData[0].backgroundColor[index] = 'rgba(255,53,0, 1)'
          this.barChartData[0].hoverBackgroundColor[index] = 'rgba(255,53,0, 1)'
        }
        if (object["gsi"] > 41) {
          this.barChartData[0].backgroundColor[index] = 'rgba(255,255,0, 1)'
          this.barChartData[0].hoverBackgroundColor[index] = 'rgba(255,255,0, 1)'
        }
        if (object["gsi"] > 60) {
          this.barChartData[0].backgroundColor[index] = 'rgba(0,223,0, 1)'
          this.barChartData[0].hoverBackgroundColor[index] = 'rgba(0,223,0, 1)'
        }
        this.barChartLabels.push(label);
        this.barChartData[0].data.push(object["gsi"])
      });
      this.apiCallSuccessful = true;
    })
  }

  public getChartHeight(): number {
    return window.innerHeight / 1.2;
  }
}