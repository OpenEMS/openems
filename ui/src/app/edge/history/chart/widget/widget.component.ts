import { Component, Input } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Service, Widget } from '../../../../shared/shared';

@Component({
  selector: WidgetComponent.SELECTOR,
  templateUrl: './widget.component.html'
})
export class WidgetComponent {

  private static readonly SELECTOR = "widget";

  @Input() public fromDate: Date;
  @Input() public toDate: Date;

  public widgetNames: string[] = [];

  constructor(
    private service: Service,
    private route: ActivatedRoute
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route);
    this.service.getWidgets().then(widgets => {
      let result: string[] = [];
      for (let widget of widgets) {
        if (!result.includes(widget.name.toString())) {
          result.push(widget.name.toString());
        }
      }
      this.widgetNames = result;
    });
  }

}
