import { Directive, ElementRef, EventEmitter, Output, inject } from "@angular/core";

/**
 * Used to react to dom changes on this element
 */
@Directive({
    selector: "[ngDomChange]",
    standalone: true,
})
export class DomChangeDirective {
    private elementRef = inject(ElementRef);


    @Output()
    public ngDomChange = new EventEmitter();

    private changes: MutationObserver;

    /** Inserted by Angular inject() migration for backwards compatibility */
    constructor(...args: unknown[]);

    constructor() {
        const element = this.elementRef.nativeElement;

        this.changes = new MutationObserver((mutations: MutationRecord[]) => {
            mutations.forEach((mutation: MutationRecord) => this.ngDomChange.emit(mutation));
        });

        this.changes.observe(element, {
            attributes: true,
            childList: true,
            characterData: true,
        });
    }
}
