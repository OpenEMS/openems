import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Service, Widget } from '../../../shared/shared';

@Component({
  selector: WidgetComponent.SELECTOR,
  templateUrl: './widget.component.html'
})
export class WidgetComponent {

  private static readonly SELECTOR = "widget";

  public widgets: Widget[] = [];

  constructor(
    private service: Service,
    private route: ActivatedRoute
  ) { }

  ngOnInit() {
    this.service.setCurrentEdge(this.route);
    this.service.getWidgets().then(widgets => this.widgets = widgets);
  }

}
