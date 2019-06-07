import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge, Service, Utils, Widget } from '../../shared/shared';

@Component({
  selector: 'live',
  templateUrl: './live.component.html'
})
export class LiveComponent implements OnInit {

  public edge: Edge = null
  public widgets: Widget[] = [];

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private service: Service,
  ) {
  }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge
    });
    this.service.getWidgets().then(widgets => this.widgets = widgets);
  }

}