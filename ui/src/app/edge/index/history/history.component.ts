import { Component, Input } from '@angular/core';
import { Edge } from '../../../shared/edge/edge';


@Component({
  selector: 'history',
  templateUrl: './history.component.html'
})
export class HistoryComponent {

  @Input() protected edge: Edge;

  // show the chart for today
  protected fromDate = new Date();
  protected toDate = new Date();

}
