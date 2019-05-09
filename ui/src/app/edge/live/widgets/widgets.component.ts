import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Service, Widget } from '../../../shared/shared';

@Component({
  selector: WidgetsComponent.SELECTOR,
  templateUrl: './widgets.component.html'
})
export class WidgetsComponent {

  private static readonly SELECTOR = "widgets";

  public widgets: Widget[] = [];

  constructor(
    private service: Service,
    private route: ActivatedRoute
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route);
    this.service.getWidgets().then(widgets => this.widgets = widgets);
  }

}
