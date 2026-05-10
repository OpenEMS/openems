import { Component, effect, Input, OnInit } from "@angular/core";
import { ModalController } from "@ionic/angular";
import { NgxSpinnerComponent } from "ngx-spinner";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { FlatWidgetButtonComponent } from "src/app/shared/components/flat/flat-widget-button/flat-widget-button";
import { Service } from "src/app/shared/shared";
import { compareOption, OptionLoader } from "./formly-lazy-select.component";
import { Option } from "./jsonrpc/getOptions";

@Component({
    selector: FormlyLazySelectModal.SELECTOR,
    templateUrl: "./formly-lazy-select.modal.html",
    imports: [
        CommonUiModule,
        NgxSpinnerComponent,
        FlatWidgetButtonComponent,
    ],
})
export class FormlyLazySelectModal implements OnInit {

    public static readonly SELECTOR = "formly-lazy-select-modal";

    @Input({ required: true })
    protected title!: string;
    @Input({ required: true })
    protected optionLoader!: OptionLoader;
    @Input()
    protected loadingText?: string;
    @Input()
    protected retryLoadingText?: string;
    @Input()
    protected missingOptionsText?: string;
    @Input()
    protected initialSelectedOption?: Option["value"];

    protected selectedOption?: Option["value"];

    protected readonly spinnerId = FormlyLazySelectModal.SELECTOR;
    protected readonly compareOption = compareOption;

    constructor(
        private service: Service,
        protected modalCtrl: ModalController,
    ) {
        effect(() => {
            if (this.optionLoader.state$().loading) {
                this.service.startSpinnerTransparentBackground(this.spinnerId);
            } else {
                this.service.stopSpinner(this.spinnerId);
            }
        });
    }

    ngOnInit(): void {
        this.selectedOption = this.initialSelectedOption;
    }

    protected triggerSearch() {
        this.optionLoader.triggerLoad();
    }

    protected handleChange(event: Event): void {
        const target = event.target as HTMLInputElement;
        this.selectedOption = target.value;
    }

    protected submitSelect() {
        this.modalCtrl.dismiss({
            value: this.selectedOption,
        });
    }

}
