import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { TextIndentation } from "src/app/shared/components/modal/modal-line/modal-line";
import { Converter } from "src/app/shared/components/shared/converter";
import { EdgeConfig } from "src/app/shared/shared";

@Component({
    selector: "DigitalOutputWidget",
    templateUrl: "./flat.html",
})
export class FlatComponent extends AbstractFlatWidget {
    protected FORMAT_SECONDS_TO_DURATION = Converter.FORMAT_SECONDS_TO_DURATION(this.translate.currentLang);
    protected fixDigitalOutputControllers: EdgeConfig.Component[] = [];
    protected singleThresholdControllers: EdgeConfig.Component[] = [];

    protected readonly TextIndentation = TextIndentation;

    protected override afterIsInitialized(): void {
        this.fixDigitalOutputControllers = this.config?.getComponentsByFactory("Controller.Io.FixDigitalOutput");
        this.singleThresholdControllers = this.config?.getComponentsByFactory("Controller.IO.ChannelSingleThreshold");
    }
}
