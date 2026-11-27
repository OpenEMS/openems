import { Component, EventEmitter, Input, Output } from "@angular/core";
import { FlatWidgetPercentagebarComponent } from "src/app/shared/components/flat/flat-widget-percentagebar/flat-widget-percentagebar";
import { environment } from "src/environments";
import { CommonUiModule } from "../../../shared/common-ui.module";
import { UpdateableState } from "./system.component";

@Component({
    selector: "oe-updateable-card",
    templateUrl: "./updateable-card.component.html",
    standalone: true,
    imports: [CommonUiModule, FlatWidgetPercentagebarComponent],
})
export class UpdateableCardComponent {
    @Input({ required: true }) public updateableState!: UpdateableState;
    @Output() public executeUpdate = new EventEmitter<UpdateableState>();

    protected showLog = false;
    protected readonly environment = environment;

    protected onExecuteUpdate(): void {
        this.executeUpdate.emit(this.updateableState);
    }

    protected getErrorMessages(errorMessage: string): string[] {
        return errorMessage
            .split(";")
            .map((message) => message.trim())
            .filter((message) => message.length > 0);
    }
}
