import { Component, Input } from "@angular/core";

@Component({
    selector: "oe-chart-legend",
    templateUrl: "./LEGEND.HTML",
    standalone: false,
})
export class ChartLegendComponent {

    @Input({ required: true }) public header: string | null = null;
    @Input({ required: true }) public description: string | null = null;
}
