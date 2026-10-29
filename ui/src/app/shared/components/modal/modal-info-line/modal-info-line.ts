import { ChangeDetectionStrategy, Component, input, Input } from "@angular/core";
import { Icon } from "src/app/shared/type/widget";

@Component({
    selector: "oe-modal-info-line",
    templateUrl: "./modal-info-line.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class ModalInfoLineComponent {
    @Input({ required: true }) public info!: { text: string; lineStyle?: string }[] | string;
    @Input() public html?: string;
    /** Icon, displayed on the left side */
    @Input({ required: true }) protected icon!: Icon;
    public customClass = input<string>();
}
