// @ts-strict-ignore
import { Component, Input } from "@angular/core";
import { Service } from "src/app/shared/shared";

/**
 * Represents an inline link
 */
@Component({
    selector: "oe-help-link",
    templateUrl: "./help-LINK.HTML",
    standalone: false,
})
export class HelpLinkComponent {

    @Input({ required: true }) public label: string | null = null;
    protected _link: string | null = null;

    constructor(private service: Service) { }

    @Input() set link(key: string | null) {
        if (!key) {
            return;
        }

        this._link =
            KEY.REPLACE("{language}", THIS.SERVICE.GET_DOCS_LANG());
    }
}
