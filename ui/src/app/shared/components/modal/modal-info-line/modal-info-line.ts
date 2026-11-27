import { ChangeDetectionStrategy, Component, input } from "@angular/core";
import { Icon } from "src/app/shared/type/widget";

export type InfoLineLink = { text: string; href?: string; callback?: () => void };

@Component({
    selector: "oe-modal-info-line",
    templateUrl: "./modal-info-line.html",
    styleUrls: ["./modal-info-line.scss"],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class ModalInfoLineComponent {
    protected readonly info = input.required<{ text: string; lineStyle?: string }[] | string>();
    protected readonly html = input<string>();
    protected readonly link = input<InfoLineLink>();

    /** Icon, displayed on the left side */
    protected readonly icon = input<Icon>();

    protected readonly lineStyle = input<string>();
    protected readonly rowStyle = input<string>();
    protected readonly customClass = input<string>();
}
