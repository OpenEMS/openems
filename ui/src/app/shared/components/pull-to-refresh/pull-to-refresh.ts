import { Component, effect, ElementRef, inject, Input, Renderer2, ChangeDetectionStrategy } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { BrowserModule } from "@angular/platform-browser";
import { RefresherCustomEvent } from "@ionic/angular";
import { DeviceDetectorService } from "ngx-device-detector";
import { NgxSpinnerModule } from "ngx-spinner";
import { PlatFormService } from "src/app/platform.service";
import { CommonUiModule } from "src/app/shared/common-ui.module";

/** Component used to indicate if live data is still updated */
@Component({
    standalone: true,
    selector: "oe-refresh-view",
    templateUrl: "./pull-to-refresh.html",
    styleUrl: "./pull-to-refresh.scss",
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [CommonUiModule, BrowserModule, NgxSpinnerModule, ReactiveFormsModule],
})
export class PullToRefreshComponent {
    @Input({ required: true }) public show: boolean = false;

    protected readonly isDesktop: boolean;

    private readonly platFormService = inject(PlatFormService);
    private readonly deviceService = inject(DeviceDetectorService);

    constructor(
        private readonly el: ElementRef,
        private readonly renderer: Renderer2,
    ) {
        this.isDesktop = this.deviceService.isDesktop();

        effect(() => {
            const isActive = this.platFormService.isActiveAgain();
            if (isActive) {
                PlatFormService.handleRefresh();
            }
        });

        // Rerender ion-content to use full available height
        const hostElement = this.el.nativeElement;
        this.renderer.addClass(hostElement, "ion-page");
    }

    @Input({ required: true }) public refresh: (ev: RefresherCustomEvent) => void = (ev: RefresherCustomEvent) => {};
}
