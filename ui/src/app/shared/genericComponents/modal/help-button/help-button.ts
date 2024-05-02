// @ts-strict-ignore
import { Component, Input } from "@angular/core";
import { Service } from "src/app/shared/shared";
import { environment } from 'src/environments';

@Component({
    selector: 'oe-help-button',
    templateUrl: './help-button.html',
})
export class HelpButtonComponent {

    protected link: string | null = null;

    constructor(private service: Service) { }

    @Input() set key(key: string) {
        if (!(key in environment.links)) {
            console.error("Key [" + key + "] not found in Environment Links");
            this.link = null;
            return;

        }
        const link = environment.links[key];
        if (link === null || link === "") {
            this.link = null;

        } else {
            this.link =
                environment.docsUrlPrefix.replace("{language}", this.service.getDocsLang())
                + environment.links[key];
        }
    }

}
