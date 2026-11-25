import { HttpClient } from "@angular/common/http";
import { Component, Input, OnChanges, SimpleChanges, inject, signal } from "@angular/core";
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
                display: grid;
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

    private readonly FALLBACK_IMG_URL: string = "assets/img/image-not-found.png";

    private readonly renderedUrl = signal<string | null>(null);
    private readonly inlineSvg = signal<SafeHtml | null>(null);

    private readonly http = inject(HttpClient);
    private readonly sanitizer = inject(DomSanitizer);

    public ngOnChanges(changes: SimpleChanges): void {
        if ("img" in changes) {
            void this.resolveImage();
        }
    }

    private onImgError(): void {
        this.renderedUrl.set(this.FALLBACK_IMG_URL);
    }

    private async resolveImage(): Promise<void> {
        const url: string | null = this.img?.url ?? null;

        this.clearSignals();

        if (url === null || !this.isSvg(url)) {
            this.renderedUrl.set(url ?? this.FALLBACK_IMG_URL);
            return;
        }

        try {
            const svgText = await firstValueFrom(this.http.get(url, { responseType: "text" }));
            if (this.isRequestOutdated(url)) {
                return;
            }
            this.inlineSvg.set(this.sanitizer.bypassSecurityTrustHtml(svgText));
        } catch {
            if (this.isRequestOutdated(url)) {
                return;
            }
            this.onImgError();
        }
    }

    private clearSignals(): void {
        this.inlineSvg.set(null);
        this.renderedUrl.set(null);
    }

    private isRequestOutdated(url: string): boolean {
        return url !== this.img?.url;
    }

    private isSvg(url: string): boolean {
        const cleanUrl = url.split("?")[0].toLowerCase();
        return cleanUrl.endsWith(".svg");
    }
}
