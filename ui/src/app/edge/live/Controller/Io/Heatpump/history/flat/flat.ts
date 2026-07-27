import { Component, ChangeDetectionStrategy } from "@angular/core";
import { EdgeConfig } from "src/app/shared/components/edge/edgeconfig";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Filter } from "src/app/shared/components/shared/filter";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { SharedControllerIoHeatpump } from "../../shared/shared";

@Component({
    selector: "controller-io-heatpump-widget",
    templateUrl: "./flat.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class ControllerIoHeatpumpFlatHistoryComponent extends AbstractFlatWidget {
    protected consumptionMeter: EdgeConfig.Component | null = null;
    protected FORMAT_SECONDS_TO_DURATION = this.Converter.FORMAT_SECONDS_TO_DURATION(this.translate.getCurrentLang());
    protected FILTER_NULL_WITH_THRESHOLD: Filter = (value: number | string | null): boolean =>
        value !== null && Number.isFinite(value) && (value as number) > 59;

    protected override afterIsInitialized(): void {
        AssertionUtils.assertIsDefined(this.config);
        AssertionUtils.assertIsDefined(this.component);
        this.consumptionMeter = SharedControllerIoHeatpump.getConsumptionMeter(this.config, this.component);
    }
}
