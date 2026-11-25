import { ChangeDetectionStrategy, Component, input } from "@angular/core";
import { IonicModule } from "@ionic/angular";

export type CardStyle = Partial<CSSStyleDeclaration> & {
    "--background"?: string;
    "--color"?: string;
};

@Component({
    selector: "oe-card",
    templateUrl: "./oe-card.html",
    styleUrl: "./oe-card.scss",
    imports: [IonicModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OeCardComponent {
    public readonly cardStyle = input<CardStyle>();
}
