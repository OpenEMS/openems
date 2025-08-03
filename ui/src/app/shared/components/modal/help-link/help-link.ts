// @ts-strict-ignore
import { Component, Input, inject } from "@angular/core";
import { Service } from "src/app/shared/shared";

/**
 * Represents an inline link
 */
@Component({
    selector: "oe-help-link",
    templateUrl: "./help-link.html",
    standalone: false,
})
export class HelpLinkComponent {
    private service = inject(Service);


    @Input({ required: true }) public label: string | null = null;
    protected _link: string | null = null;

    /** Inserted by Angular inject() migration for backwards compatibility */
    constructor(...args: unknown[]);

    constructor() { }

    @Input() set link(key: string | null) {
        if (!key) {
            return;
        }

        this._link =
            key.replace("{language}", this.service.getDocsLang());
    }
}
