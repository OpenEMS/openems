import { CommonModule } from "@angular/common";
import { Component, Input } from "@angular/core";
import { IonicModule } from "@ionic/angular";

@Component({
    selector: "oe-img",
    templateUrl: "./oe-img.html",
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

    protected readonly FALLBACK_IMG_URL: string = "assets/img/image-not-found.png";

    protected onImgError(event: Event) {
        const imgElement = event.target as HTMLImageElement;
        imgElement.src = this.FALLBACK_IMG_URL;
    }
}
