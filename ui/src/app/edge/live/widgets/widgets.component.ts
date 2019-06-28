import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Service, Widget, WidgetNature } from '../../../shared/shared';

@Component({
  selector: WidgetsComponent.SELECTOR,
  templateUrl: './widgets.component.html'
})
export class WidgetsComponent {

  private static readonly SELECTOR = "widgets";

  public widgets: Widget[] = [];
  public evcsWidgets: number = 0;

  constructor(
    private service: Service,
    private route: ActivatedRoute
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route);
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
