// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Converter } from "src/app/shared/components/shared/converter";
import { Filter } from "src/app/shared/components/shared/filter";

@Component({
    selector: "gridOptimizedChargeWidget",
    templateUrl: "./flat.html",
})
export class FlatComponent extends AbstractFlatWidget {
    protected FORMAT_SECONDS_TO_DURATION = Converter.FORMAT_SECONDS_TO_DURATION(this.translate.currentLang);
    protected filter: Filter = (value: number): boolean => value > 59;
}
