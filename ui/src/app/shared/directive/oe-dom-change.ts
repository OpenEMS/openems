import { Directive, ElementRef, EventEmitter, Output } from "@angular/core";

/**
 * Used to react to dom changes on this element
 */
@Directive({
    selector: "[ngDomChange]",
    standalone: true,
})
export class DomChangeDirective {

    @Output()
    public ngDomChange = new EventEmitter();

    private changes: MutationObserver;

    constructor(private elementRef: ElementRef) {
        const element = this.elementRef.nativeElement;

        this.changes = new MutationObserver((mutations: MutationRecord[]) => {
            mutations.forEach((mutation: MutationRecord) => this.ngDomChange.emit(mutation));
        });

        this.changes.observe(element, {
            attributes: true,
            childList: true,
            characterData: true,
            subtree: true,
        });
    }
}
