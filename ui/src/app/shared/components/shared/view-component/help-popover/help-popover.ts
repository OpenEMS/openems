import { Component, EventEmitter, Input, Output } from "@angular/core";
import { IonicModule } from "@ionic/angular";
import { v4 as uuidv4 } from "uuid";

@Component({
    selector: "oe-help-popover-button",
    templateUrl: "./help-POPOVER.HTML",
    standalone: true,
    imports: [
        IonicModule,
    ],
})
export class HelpPopoverButtonComponent {

    @Input({ required: true }) public helpMsg: string | null = null;
    @Output() public didPopoverDismiss: EventEmitter<any> = new EventEmitter();

    protected uuid: string = uuidv4();

};
