import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Service } from '../../../shared/shared';
import { WidgetNature, Widget, WidgetFactory } from './widget';

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
    this.setWidgets();
  }

  /**
   * Defines the widgets that should be shown.
   */
  private setWidgets() {
    this.service.getConfig().then(config => {
      let widgets = [];
      for (let nature of Object.keys(WidgetNature)) {
        for (let componentId of config.getComponentsImplementingNature(nature)) {
          widgets.push({ name: nature, componentId: componentId })
        }
      }
      for (let factory of Object.keys(WidgetFactory)) {
        for (let componentId of config.getComponentIdsByFactory(factory)) {
          widgets.push({ name: factory, componentId: componentId })
        }
      }
      this.widgets = widgets;
    })
  }
}
