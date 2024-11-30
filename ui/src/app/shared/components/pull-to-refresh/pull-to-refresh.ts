import { CommonModule } from "@angular/common";
import { Component, ElementRef, Input, Renderer2 } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { IonicModule, RefresherCustomEvent } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { NgxSpinnerModule } from "ngx-spinner";

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
            <ion-item lines="none" color="medium-tint" style="background-color: lightgray !important; cursor: pointer;"
                class="ion-item-min-content-height">
                <ion-row class="ion-justify-content-center ion-full-width ion-align-items-center ion-no-padding">
                    <ion-col class="ion-text-align-center ion-col-with-left-and-right-icon">
                        <ion-icon name="arrow-down-circle-outline" size="medium"></ion-icon>
                        <ion-text class="ion-font-size-smaller ion-font-style-oblique" translate>
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

    constructor(private el: ElementRef, private renderer: Renderer2) {

        // Rerender ion-content to use full available height
        const hostElement = this.el.nativeElement;
        this.renderer.addClass(hostElement, "ion-page");
    }

    @Input({ required: true }) public refresh: (ev: RefresherCustomEvent) => void = (ev: RefresherCustomEvent) => { };
}
