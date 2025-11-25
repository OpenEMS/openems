import { Component, computed, effect, inject, Signal, signal, WritableSignal } from "@angular/core";
import { ModalController } from "@ionic/angular";
import { FieldType } from "@ngx-formly/core";
import { FormlyFieldProps } from "@ngx-formly/ionic/form-field";
import { concatMap, finalize, Subject, take, takeUntil, tap, timer } from "rxjs";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { JsonRpcUtils } from "src/app/shared/jsonrpc/jsonrpcutils";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { Edge, Service, Websocket } from "src/app/shared/shared";
import { FormlyFieldConfigWithInitialModel } from "../../jsonrpc/getAppAssistant";
import { FormlyLazySelectModal } from "./formly-lazy-select.modal";
import { GetOptions, Option } from "./jsonrpc/getOptions";


@Component({
    selector: FormlyLazySelectComponent.SELECTOR,
    templateUrl: "./formly-lazy-select.component.html",
    imports: [
        CommonUiModule,
        FormlyLazySelectModal,
    ],
})
export class FormlyLazySelectComponent extends FieldType<FormlyFieldConfigWithInitialModel<FormlyFieldProps & {
    componentId?: string,
    method?: string,
    loadingText?: string,
    retryLoadingText?: string,
    missingOptionsText?: string,
}>> {

    public static readonly SELECTOR = "formly-lazy-select";

    protected loading: boolean = false;
    protected selectionOpen = signal(false);
    protected currentOptions: { name: string, value: string }[] = [];
    protected displayValue = computed(() => {
        this.valueChange();
        const value = this.formControl.value;
        const selectedOption = this.optionLoader.state$().options.find(e => compareOption(e.value, value));
        return selectedOption?.name ?? value?.name ?? "";
    });

    private readonly websocket = inject(Websocket);
    private readonly service = inject(Service);
    private readonly modalController = inject(ModalController);

    // emittet when the value changes, updates the display value
    private readonly valueChange: WritableSignal<null> = signal(null, { equal: (a, b) => false });

    private optionLoader = new OptionLoader(() => this.loadOptions());
    private edge: Edge | null = null;

    constructor() {
        super();
        effect((onCleanup) => {
            const componentId = this.props.componentId;
            const method = this.props.method;
            if (componentId == null || method == null) {
                return;
            }

            this.edge = this.service.currentEdge();
            if (this.edge == null) {
                return;
            }
            this.optionLoader = new OptionLoader(() => this.loadOptions());

            if (this.formControl.value == null) {
                this.optionLoader.triggerLoad();
            }

            onCleanup(() => {
                this.optionLoader.destroy();
            });
        });
    }

    protected async onClick(): Promise<void> {
        if (this.selectionOpen()) {
            return;
        }
        this.selectionOpen.set(true);
        const modal = await this.modalController.create({
            component: FormlyLazySelectModal,
            componentProps: {
                title: this.props.label || "",
                optionLoader: this.optionLoader,
                initialSelectedOption: this.formControl.value,
                loadingText: this.props.loadingText,
                retryLoadingText: this.props.retryLoadingText,
                missingOptionsText: this.props.missingOptionsText,
            },
            cssClass: "auto-height",
        });
        modal.onDidDismiss().then(event => {
            this.selectionOpen.set(false);
            if (event.data == null) {
                return;
            }
            const selectedValue = event.data.value;

            this.formControl.setValue(selectedValue);
            this.formControl.markAsDirty();
            this.valueChange.set(null);
        });

        return await modal.present();
    }

    protected async loadOptions(): Promise<Option[]> {
        const componentId = this.props.componentId;
        const method = this.props.method;
        const edge = this.edge;
        if (componentId == null || method == null || edge == null) {
            return [];
        }

        const [error, response] = await JsonRpcUtils.handle<GetOptions.Response>(edge.sendRequest(this.websocket, new ComponentJsonApiRequest({
            componentId: componentId,
            payload: new GetOptions.Request(method, {
                ...(this.field.instanceId && { forInstance: this.field.instanceId }),
            }),
        })));

        if (error != null || response == null) {
            return [];
        }

        return response.result.options;
    }

}

export function compareOption(o1: Option["value"], o2: Option["value"]): boolean {
    return JSON.stringify(o1) === JSON.stringify(o2);
}

export type OptionLoaderState = {
    loading: boolean,
    options: Option[],
};

export class OptionLoader {
    private static readonly NUMBER_OF_REFRESHES: number = 10;
    private static readonly WAIT_BETWEEN_REFRESHES: number = 5000; // in ms

    public readonly state$: Signal<OptionLoaderState>;

    private readonly unsubscribe = new Subject();
    private readonly state: WritableSignal<OptionLoaderState> = signal({ loading: false, options: [] });

    constructor(
        private loadOptions: () => Promise<Option[]>,
    ) {
        this.state$ = this.state.asReadonly();
    }

    public triggerLoad() {
        const source = timer(0, OptionLoader.WAIT_BETWEEN_REFRESHES);
        source.pipe(
            take(OptionLoader.NUMBER_OF_REFRESHES),
            takeUntil(this.unsubscribe),
            tap(() => {
                this.state.update(state => {
                    return { ...state, loading: true };
                });
            }),
            concatMap(_ => {
                return this.loadOptions();
            }),
            tap(options => {
                this.state.update(state => {
                    return { ...state, options: options };
                });
            }),
            finalize(() => {
                this.state.update(state => {
                    return { ...state, loading: false };
                });
            }),
        ).subscribe();
    }

    public destroy() {
        this.unsubscribe.next(null);
        this.unsubscribe.complete();
    }

}
