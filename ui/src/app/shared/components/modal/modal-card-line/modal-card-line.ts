import { ChangeDetectionStrategy, Component, input } from "@angular/core";
import { IonicModule } from "@ionic/angular";
import type { Icon } from "src/app/shared/type/widget";
import { OeCardComponent, type CardStyle } from "../../oe-card/oe-card";
import { OeLinkComponent, type OeLink } from "../../oe-link/oe-link";

@Component({
    selector: "oe-modal-card-line",
    templateUrl: "./modal-card-line.html",
    imports: [IonicModule, OeCardComponent, OeLinkComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ModalCardLineComponent {
    public readonly text = input<string>();
    public readonly html = input<string>();
    public readonly icon = input<Icon>();
    public readonly cardStyle = input<CardStyle>();
    public readonly link = input<OeLink>();
}
