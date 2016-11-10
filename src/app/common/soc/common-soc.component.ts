import { Component, OnInit, Input } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { ISubscription } from 'rxjs/Subscription';

@Component({
  selector: 'common-soc',
  templateUrl: './common-soc.component.html',
  styleUrls: ['./common-soc.component.css']
})
export class CommonSocComponent {
  private _soc :number = null;

  @Input()
  set soc(soc: number) {
    this._soc = soc;
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
            "#FF6384",
            "#36A2EB"
          ],
          hoverBackgroundColor: [
            "#FF6384",
            "#36A2EB"
          ]
        }]
    };
    this.chartoptions = {
      tooltips: {
        enabled: false
      },
      legend: {
        display: false
      }
    };
  }
}
