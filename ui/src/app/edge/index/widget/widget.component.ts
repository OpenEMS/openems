import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Service, Widget, Edge } from '../../../shared/shared';

@Component({
  selector: WidgetComponent.SELECTOR,
  templateUrl: './widget.component.html'
})
export class WidgetComponent {

  private static readonly SELECTOR = "widget";

  public widgets: Widget[] = [];
  public edge: Edge = null;

  constructor(
    private service: Service,
    private route: ActivatedRoute
  ) { }

  ngOnInit() {
    this.service.setCurrentEdge(this.route).then(edge => {
      this.edge = edge;
    });
    this.service.getWidgets().then(widgets => this.widgets = widgets);
  }

}
