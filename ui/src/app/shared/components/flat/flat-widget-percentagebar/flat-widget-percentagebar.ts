import { Component, Input } from "@angular/core";
import { AbstractFlatWidgetLine } from "../abstract-flat-widget-line";

@Component({
    selector: "oe-flat-widget-percentagebar",
    templateUrl: "./flat-widget-percentagebar.html",
})
export class FlatWidgetPercentagebarComponent extends AbstractFlatWidgetLine { 
    /*
    * Secondary information that gets displayed in parentheses behind the percentage value.
    */
    @Input()    
    public secondaryValue:string | null = null;
}
