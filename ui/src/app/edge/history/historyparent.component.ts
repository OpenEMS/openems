import { Component, ChangeDetectionStrategy } from "@angular/core";

/** This component is needed as a routing parent and acts as a transit station without being displayed. */
@Component({
    selector: "edge",
    template: " <ion-router-outlet> </ion-router-outlet> ",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class HistoryParentComponent {}
