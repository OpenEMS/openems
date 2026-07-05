import { Component, Input } from "@angular/core";
import { CommonUiModule } from "../../common-ui.module";
import { ComponentsBaseModule } from "../components.module";
import { OeFormlyField } from "../shared/oe-formly-component";

@Component({
    selector: "oe-stats-line",
    templateUrl: "./stats.html",
    standalone: true,
    imports: [CommonUiModule, ComponentsBaseModule],
})
export class StatsComponent {
    @Input({ required: true }) public stats: Stat[] = [];
}

export type Stat =
    | Omit<OeFormlyField.ValueFromChannelsLine, "type">
    | {
          name: string;
          value: number;
          unit: string;
          predictionValue?: number;
      };
