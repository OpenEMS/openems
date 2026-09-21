import { HttpClient } from "@angular/common/http";
import { Component, Input, OnChanges, SimpleChanges, inject } from "@angular/core";
import { DomSanitizer, SafeHtml } from "@angular/platform-browser";
import { IonicModule } from "@ionic/angular";
import { firstValueFrom } from "rxjs";
import { TIntRange } from "../../type/utility";

@Component({
    selector: "oe-img",
    templateUrl: "./oe-img.html",
    standalone: true,
    imports: [IonicModule],
    styles: [
        `
            :host {
                display: block;
            }

            .svg-container {
                display: block;
            }

            .svg-container :where(svg) {
                display: block;
                width: 100%;
                height: 100%;
            }
        `,
    ],
})
export class OeImageComponent implements OnChanges {
    @Input({ required: true }) public img!: {
        url: string | null;
        width?: TIntRange<1, 101>;
        height?: TIntRange<1, 101>;
        color?: string;
        style?: Exclude<Partial<CSSStyleDeclaration>, "objectFit" | "width" | "height" | "src">;
    } | null;

    protected readonly FALLBACK_IMG_URL: string = "assets/img/image-not-found.png";

    protected renderedUrl: string = this.FALLBACK_IMG_URL;
    protected inlineSvg: SafeHtml | null = null;

    private readonly http = inject(HttpClient);
    private readonly sanitizer = inject(DomSanitizer);

    public ngOnChanges(changes: SimpleChanges): void {
        if ("img" in changes) {
            void this.resolveImage();
        }
    }

    protected onImgError(): void {
        this.renderedUrl = this.FALLBACK_IMG_URL;
    }

    private async resolveImage(): Promise<void> {
        const url = this.img?.url;
        this.inlineSvg = null;
        this.renderedUrl = url ?? this.FALLBACK_IMG_URL;

        if (url == null || !this.isSvg(url)) {
            return;
        }

        try {
            const svgText = await firstValueFrom(this.http.get(url, { responseType: "text" }));
            this.inlineSvg = this.sanitizer.bypassSecurityTrustHtml(svgText);
        } catch {
            this.renderedUrl = this.FALLBACK_IMG_URL;
        }
    }

    private isSvg(url: string): boolean {
        const cleanUrl = url.split("?")[0].toLowerCase();
        return cleanUrl.endsWith(".svg");
    }
}
