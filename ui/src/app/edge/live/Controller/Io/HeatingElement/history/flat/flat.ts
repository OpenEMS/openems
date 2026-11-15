import { ChangeDetectionStrategy, Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { EdgeConfig } from "src/app/shared/shared";
import { HeatingElementComponent } from "../../util/utils";

@Component({
    selector: "controller-io-heatingelement-widget",
    templateUrl: "./flat.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class ControllerHeatingElementChartComponent extends AbstractFlatWidget {
    protected heatingElementConfig: HeatingElementComponent = new HeatingElementComponent(null);
    protected levels: number[] = this.heatingElementConfig.getActiveLevels();
    protected FORMAT_SECONDS_TO_DURATION = this.Converter.FORMAT_SECONDS_TO_DURATION(this.translate.getCurrentLang());
    protected consumptionMeter: EdgeConfig.Component = new EdgeConfig.Component();

    protected override afterIsInitialized(): void {
        if (this.component == null) {
            return;
        }

        this.heatingElementConfig = new HeatingElementComponent(this.component, this.edge);
        this.levels = this.heatingElementConfig.getActiveLevels();

        this.consumptionMeter =
            this.config?.getComponent(this.component.properties["meter.id"]) ?? new EdgeConfig.Component();
    }
}
