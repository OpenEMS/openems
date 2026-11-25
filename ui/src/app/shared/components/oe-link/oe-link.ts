import { ChangeDetectionStrategy, Component, input } from "@angular/core";

export type OeLink = {
    text: string;
    href?: string;
    callback?: () => void;
};

@Component({
    selector: "oe-link",
    templateUrl: "./oe-link.html",
    styleUrl: "./oe-link.scss",
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OeLinkComponent {
    public readonly link = input.required<OeLink>();
}
