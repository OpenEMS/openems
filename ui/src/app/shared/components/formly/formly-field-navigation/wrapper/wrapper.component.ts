import { afterNextRender, Component, ElementRef, input, signal, viewChild } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { DomChangeDirective } from "src/app/shared/directive/oe-dom-change";
import { OeFormlyField } from "../../../shared/oe-formly-component";

@Component({
    selector: "oe-formly-wrapper",
    standalone: true,
    template: `
        <div
            (ngDomChange)="onDomChange()"
            #projectedContent
            [class]="class() ?? ''"
            [style.padding-top]="hasContent() != null && hasContent() ? '1%' : '0'"
        >
            <ng-content></ng-content>
        </div>
    `,
    imports: [CommonUiModule, DomChangeDirective],
})
export class FormlyWrapperComponent {
    public readonly class = input<OeFormlyField["cssClass"]>();
    protected readonly hasContent = signal<boolean | null>(null);
    private readonly content = viewChild.required<ElementRef<HTMLElement>>("projectedContent");

    constructor() {
        afterNextRender(() => {
            this.checkContent();
        });
    }
    protected onDomChange(): void {
        this.checkContent();
    }

    private checkContent(): void {
        const el = this.content().nativeElement;
        const empty = Array.from(el.children).every((child) => {
            const rect = child.getBoundingClientRect();
            return rect.width === 0 && rect.height === 0;
        });
        this.hasContent.set(!empty);
    }
}
