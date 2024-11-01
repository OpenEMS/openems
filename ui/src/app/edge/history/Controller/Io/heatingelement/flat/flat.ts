import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";

@Component({
    selector: "controller-io-heatingelement-widget",
    templateUrl: "./flat.html",
})
export class FlatComponent extends AbstractFlatWidget {
    protected FORMAT_SECONDS_TO_DURATION = this.Converter.FORMAT_SECONDS_TO_DURATION(this.translate.currentLang);
}
