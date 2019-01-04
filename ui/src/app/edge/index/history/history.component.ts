import { Component, Input } from '@angular/core';
import { Edge } from '../../../shared/edge/edge';
import { Service } from '../../../shared/service/service';
import { ActivatedRoute } from '@angular/router';

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
