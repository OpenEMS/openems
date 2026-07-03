import { CommonModule } from "@angular/common";
import { Component, effect, inject } from "@angular/core";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { Service } from "src/app/shared/shared";

interface FenaMessage {
    text: string;
    actions: string[];
}

@Component({
    selector: "oe-fena",
    standalone: true,
    imports: [
        CommonModule,
        IonicModule,
        TranslateModule,
    ],
    templateUrl: "./fena.component.html",
    styleUrl: "./fena.component.scss",
})
export class FenaComponent {

    protected messages: FenaMessage[] = [];

    private readonly service: Service = inject(Service);

    constructor() {
        effect(() => {
            // Get edge for future enhancements
            const _edge = this.service.currentEdge();
            this.messages = [];
        });
    }
}
