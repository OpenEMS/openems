import { Component, Input } from '@angular/core';

@Component({
  selector: 'common-soc',
  templateUrl: './common-soc.component.html'
})
export class CommonSocComponent {
  @Input()
  private height: number = 200;

  @Input()
  set soc(soc: number) {
    this.data = [soc, 100 - soc];
  }

  private data: number[];
  private options: any;
  private colors: Array<any>;
  private labels: string[] = ['Ladezustand', ''];
  private chartType: string = 'doughnut';

  constructor() {
    this.colors = [
      {
        backgroundColor: ["#2D8FAB", "#E8E8E8"],
        hoverBackgroundColor: ["#2D8FAB", "#E8E8E8"]
      }
    ];
    this.options = {
      animation: false,
      tooltips: {
        enabled: false
      },
      legend: {
        display: false
      },
      responsive: true,
      maintainAspectRatio: false
    };
  }
}
