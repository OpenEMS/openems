import { CommonModule } from "@angular/common";
import { Component, effect, EventEmitter, Output } from "@angular/core";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { FlatWidgetButtonComponent } from "src/app/shared/components/flat/flat-widget-button/flat-widget-button";
import { ModalComponentsModule } from "src/app/shared/components/modal/modal.module";
import { Edge, Service } from "src/app/shared/shared";
import { ObjectUtils } from "src/app/shared/utils/object/object.utils";
import { EdgeSettings } from "../../../edge";

@Component({
    selector: "oe-marketing-annual-review-button",
    template: `
      <ion-button class="ion-white-space-nowrap ion-custom-padding ion-text-bold" (click)="navigateTo()"
          color="primary">
          {{'POPOVER.ANNUAL_REVIEW.BUTTON_TEXT' | translate}}
        </ion-button>
    `,
    imports: [
        CommonModule,
        IonicModule,
        ModalComponentsModule,
        TranslateModule,
        FlatWidgetButtonComponent,
    ],
})

export class MarketingAnnualReviewButtonComponent {
    @Output() public modalDismiss: EventEmitter<void> = new EventEmitter();
    protected link: FlatWidgetButtonComponent["link"] | null = null;

    constructor(private service: Service) {
        effect(() => {
            const edge = this.service.currentEdge();

            if (edge == null) {
                return;
            }

            this.link = MarketingAnnualReviewButtonComponent.buildLink(edge);
        });
    }

    public static buildLink(edge: Edge): FlatWidgetButtonComponent["link"] | null {
        const uuid = ObjectUtils.getKeySafely(edge.settings, EdgeSettings.ANNUAL_REVIEW_2025);

        if (uuid == null) {
            return null;
        }
        return { text: `https://fenecon.de/fems-wrapped-2025/${uuid}` };
    }

    protected navigateTo() {
        if (this.link == null) {
            return;
        }
        this.modalDismiss.emit();
        window.open(this.link.text, "_blank");
    }
}
