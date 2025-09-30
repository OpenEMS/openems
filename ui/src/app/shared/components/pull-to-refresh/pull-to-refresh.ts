import { CommonModule } from "@angular/common";
import { Component, effect, ElementRef, Input, Renderer2 } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { IonicModule, RefresherCustomEvent } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { NgxSpinnerModule } from "ngx-spinner";
import { PlatFormService } from "src/app/PLATFORM.SERVICE";

/**
 * Component used to indicate if live data is still updated
 */
@Component({
    standalone: true,
    selector: "oe-refresh-view",
    template: `
        <ng-container *ngIf="show">
            <ion-refresher mode="ios" slot="fixed" (ionRefresh)="refresh($event)">
                <ion-refresher-content refreshingSpinner="crescent"></ion-refresher-content>
            </ion-refresher>
            <ion-item lines="none" color="medium-tint" style="background-color: var(--ion-color-background-pull-to-refresh) !important; cursor: pointer;"
                class="ion-item-min-content-height">
                <ion-row class="ion-justify-content-center ion-full-width ion-align-items-center ion-no-padding">
                    <ion-col class="ion-text-align-center ion-col-with-left-and-right-icon">
                        <ion-icon name="arrow-down-circle-outline" size="medium" style="color: var(--ion-color-pull-to-refresh)"></ion-icon>
                        <ion-text class="ion-font-size-smaller ion-font-style-oblique" style="color: var(--ion-color-pull-to-refresh);" translate>
                            LIVE.PULL_TO_REFRESH
                        </ion-text>
                        <ion-icon name="arrow-down-circle-outline"></ion-icon>
                    </ion-col>
                </ion-row>
            </ion-item>
        </ng-container>
        <ion-content>
            <ng-content></ng-content>
        </ion-content>
    `,
    styles: `
        .ion-col-with-left-and-right-icon {
            display: flex;
            justify-content: center;

            ion-text {
                padding-left: 1%;
                padding-right: 1%;
                align-self: center;
            }
        }
    `,
    imports: [
        BrowserModule,
        BrowserAnimationsModule,
        IonicModule,
        TranslateModule,
        CommonModule,
        NgxSpinnerModule,
        ReactiveFormsModule,
    ],
})
export class PullToRefreshComponent {
    @Input({ required: true }) public show: boolean = false;

    constructor(private el: ElementRef, private renderer: Renderer2, private platFormService: PlatFormService) {

        effect(() => {
            const isActive = PLAT_FORM_SERVICE.IS_ACTIVE_AGAIN();
            if (isActive) {
                PLAT_FORM_SERVICE.HANDLE_REFRESH();
            }
        });

        // Rerender ion-content to use full available height
        const hostElement = THIS.EL.NATIVE_ELEMENT;
        THIS.RENDERER.ADD_CLASS(hostElement, "ion-page");
    }

    @Input({ required: true }) public refresh: (ev: RefresherCustomEvent) => void = (ev: RefresherCustomEvent) => { };

}
