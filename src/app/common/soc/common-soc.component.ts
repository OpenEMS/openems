import { Component, OnInit, Input } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { ISubscription } from 'rxjs/Subscription';

@Component({
  selector: 'common-soc',
  templateUrl: './common-soc.component.html',
  styleUrls: ['./common-soc.component.css']
})
export class CommonSocComponent {
  @Input()
  private height: number = 200;

  @Input()
  set soc(soc: number) {
    this.chartdata.datasets[0].data = [soc, 100-soc];
  }

  private chartdata: any;
  private chartoptions: any;
  constructor() {
    this.chartdata = {
      labels: ['Ladezustand', ''],
      datasets: [
        {
          data: [null, null],
          backgroundColor: [
            "#2D8FAB",
            "#E8E8E8"
          ],
          hoverBackgroundColor: [
            "#2D8FAB",
            "#E8E8E8"
          ]
        }]
    };
    this.chartoptions = {
      tooltips: {
        enabled: false
      },
      legend: {
        display: false
      },
      responsive: true,
      maintainAspectRatio: true
    };
  }
}
