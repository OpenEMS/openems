import { Directive, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges } from "@angular/core";

@Directive({
    selector: "[ngDomChange]",
    standalone: true,
})
export class DomChangeDirective implements OnChanges, OnDestroy {
    @Output() public ngDomChange = new EventEmitter<MutationRecord>();

    @Input() public ngDomChangeObserveAttributes: boolean = true;

    private changes: MutationObserver;

    constructor(private elementRef: ElementRef<HTMLElement>) {
        this.changes = new MutationObserver((mutations) => {
            for (const m of mutations) { this.ngDomChange.emit(m); }
        });

        this.startObserving();
    }

    ngOnChanges(changes: SimpleChanges): void {
        if ("ngDomChangeObserveAttributes" in changes) {
            this.startObserving();
        }
    }

    ngOnDestroy(): void {
        this.changes.disconnect();
    }

    private startObserving(): void {
        const el = this.elementRef.nativeElement;
        this.changes.disconnect();

        this.changes.observe(el, {
            attributes: this.ngDomChangeObserveAttributes,
            childList: true,
            characterData: true,
            subtree: true,
        });
    }
}
