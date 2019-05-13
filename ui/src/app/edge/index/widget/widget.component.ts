import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Service, Widget, WidgetNature } from '../../../shared/shared';

@Component({
  selector: WidgetComponent.SELECTOR,
  templateUrl: './widget.component.html'
})
export class WidgetComponent {

  private static readonly SELECTOR = "widget";

  public widgets: Widget[] = [];
  public evcsWidgets: number = 0;

  constructor(
    private service: Service,
    private route: ActivatedRoute
  ) { }

  ngOnInit() {
    this.service.setCurrentEdge(this.route);
    this.service.getWidgets().then(widgets => {
      this.widgets = widgets;
      this.widgets.forEach(widget => {
        if (widget.name.toString() == "io.openems.edge.evcs.api.Evcs") {
          this.evcsWidgets++;
        }
      });
    });

  }

  public haveEvcsWidget(): boolean {
    return this.evcsWidgets > 0;
  }
}
