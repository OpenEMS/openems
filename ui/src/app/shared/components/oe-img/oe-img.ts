import { Component, Input } from "@angular/core";
import { IonicModule } from "@ionic/angular";
import { TIntRange } from "../../type/utility";

@Component({
    selector: "oe-img",
    templateUrl: "./oe-img.html",
    standalone: true,
    imports: [
        IonicModule,
    ],
})
export class OeImageComponent {

    @Input({ required: true }) public img!: {
        url: string | null,
        width?: TIntRange<1, 101>,
        height?: TIntRange<1, 101>,
        style?: Exclude<Partial<CSSStyleDeclaration>, "objectFit" | "width" | "height" | "src">,
    } | null;

    protected readonly FALLBACK_IMG_URL: string = "assets/img/image-not-found.png";

    protected onImgError(event: Event) {
        const imgElement = event.target as HTMLImageElement;
        imgElement.src = this.FALLBACK_IMG_URL;
    }
}
