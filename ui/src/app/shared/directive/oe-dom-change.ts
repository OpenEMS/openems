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
        const element = THIS.ELEMENT_REF.NATIVE_ELEMENT;

        THIS.CHANGES = new MutationObserver((mutations: MutationRecord[]) => {
            MUTATIONS.FOR_EACH((mutation: MutationRecord) => THIS.NG_DOM_CHANGE.EMIT(mutation));
        });

        THIS.CHANGES.OBSERVE(element, {
            attributes: true,
            childList: true,
            characterData: true,
        });
    }
}
