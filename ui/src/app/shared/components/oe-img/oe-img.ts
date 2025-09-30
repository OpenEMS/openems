import { CommonModule } from "@angular/common";
import { Component, Input } from "@angular/core";
import { IonicModule } from "@ionic/angular";

@Component({
    selector: "oe-img",
    templateUrl: "./oe-IMG.HTML",
    standalone: true,
    imports: [
        CommonModule,
        IonicModule,
    ],
})
export class OeImageComponent {

    @Input({ required: true }) public img!: {
        url: string,
        width?: number,
        height?: number,
    } | null;

    protected readonly FALLBACK_IMG_URL: string = "assets/img/image-not-FOUND.PNG";

    protected onImgError(event: Event) {
        const imgElement = EVENT.TARGET as HTMLImageElement;
        IMG_ELEMENT.SRC = this.FALLBACK_IMG_URL;
    }
}
