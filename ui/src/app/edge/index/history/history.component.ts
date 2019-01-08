import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge, Service } from '../../../shared/shared';

@Component({
  selector: 'history',
  templateUrl: './history.component.html'
})
export class HistoryComponent {

  // show the chart for today
  protected fromDate = new Date();
  protected toDate = new Date();

  protected edge: Edge;

  constructor(
    private service: Service,
    private route: ActivatedRoute
  ) { }

  ngOnInit() {
    this.service.setCurrentEdge(this.route).then(edge => this.edge = edge);
  }
}
