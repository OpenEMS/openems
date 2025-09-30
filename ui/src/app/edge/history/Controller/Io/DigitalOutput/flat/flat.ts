import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { TextIndentation } from "src/app/shared/components/modal/modal-line/modal-line";
import { Converter } from "src/app/shared/components/shared/converter";
import { EdgeConfig } from "src/app/shared/shared";

@Component({
    selector: "DigitalOutputWidget",
    templateUrl: "./FLAT.HTML",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {
    protected FORMAT_SECONDS_TO_DURATION = Converter.FORMAT_SECONDS_TO_DURATION(THIS.TRANSLATE.CURRENT_LANG);
    protected fixDigitalOutputControllers: EDGE_CONFIG.COMPONENT[] = [];
    protected singleThresholdControllers: EDGE_CONFIG.COMPONENT[] = [];

    protected readonly TextIndentation = TextIndentation;

    protected override afterIsInitialized(): void {
        THIS.FIX_DIGITAL_OUTPUT_CONTROLLERS = THIS.CONFIG?.getComponentsByFactory("CONTROLLER.IO.FIX_DIGITAL_OUTPUT");
        THIS.SINGLE_THRESHOLD_CONTROLLERS = THIS.CONFIG?.getComponentsByFactory("CONTROLLER.IO.CHANNEL_SINGLE_THRESHOLD");
    }
}
