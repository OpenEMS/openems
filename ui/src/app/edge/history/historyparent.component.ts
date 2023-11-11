import { Component } from "@angular/core";

/*** This component is needed as a routing parent and acts as a transit station without being displayed.*/
@Component({
    selector: "edge",
    template: `
    <ion-router-outlet>
</ion-router-outlet>
    `
})
export class HistoryParentComponent { }
