import { Component } from "@angular/core";
import { AbstractFlatWidgetLine } from "../../abstract-flat-widget-line";

@Component({
  /** If multiple items in line use this */
  selector: "oe-flat-widget-line-item",
  templateUrl: "./flat-widget-line-item.html"
})
export class FlatWidgetLineItemComponent extends AbstractFlatWidgetLine { }