import { ChangeDetectorRef, Directive, Inject, Input, OnDestroy, OnInit } from "@angular/core";
import { FormBuilder, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { UUID } from "angular2-uuid";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";

@Directive()
export abstract class AbstractModal implements OnInit, OnDestroy {

    @Input() component: EdgeConfig.Component = null;
    @Input() formGroup: FormGroup = null;
    @Input() controlName: string;

    public isInitialized: boolean = false;
    public edge: Edge = null;
    public config: EdgeConfig = null;
    public stopOnDestroy: Subject<void> = new Subject<void>();

    private selector: string = UUID.UUID().toString();

    constructor(
        @Inject(Websocket) protected websocket: Websocket,
        @Inject(ActivatedRoute) protected route: ActivatedRoute,
        @Inject(Service) protected service: Service,
        @Inject(ModalController) public modalCtrl: ModalController,
        @Inject(TranslateService) protected translate: TranslateService,
        @Inject(FormBuilder) public formBuilder: FormBuilder,
        private ref: ChangeDetectorRef

    ) {
        ref.detach();
        setInterval(() => {
            this.ref.detectChanges(); // manually trigger change detection
        }, 0);
    }

    public ngOnInit() {
        // this.getFormGroup()
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.service.getConfig().then(config => {

                // store important variables publically
                this.edge = edge;
                this.config = config;

                // If component is passed
                if (this.component != null) {
                    this.component = config.components[this.component.id];

                    // announce initialized
                    this.isInitialized = true;

                    // get the channel addresses that should be subscribed
                    let channelAddresses: ChannelAddress[] = this.getChannelAddresses();
                    let channelIds = this.getChannelIds();
                    for (let channelId of channelIds) {
                        channelAddresses.push(new ChannelAddress(this.component.id, channelId));
                    }
                    if (channelAddresses.length != 0) {
                        this.edge.subscribeChannels(this.websocket, this.selector, channelAddresses);
                    }

                    // call onCurrentData() with latest data
                    edge.currentData.pipe(takeUntil(this.stopOnDestroy)).subscribe(currentData => {
                        let allComponents = {};
                        let thisComponent = {};
                        for (let channelAddress of channelAddresses) {
                            let ca = channelAddress.toString();
                            allComponents[ca] = currentData.channel[ca];
                            if (channelAddress.componentId === this.component.id) {
                                thisComponent[channelAddress.channelId] = currentData.channel[ca];
                            }
                        }
                        this.onCurrentData({ thisComponent: thisComponent, allComponents: allComponents });
                    });
                }
            });
        });
    };

    public ngOnDestroy() {
        // Unsubscribe from OpenEMS
        this.edge.unsubscribeChannels(this.websocket, this.selector);

        // Unsubscribe from CurrentData subject
        this.stopOnDestroy.next();
        this.stopOnDestroy.complete();
    }

    /**
     * Applies all Value-Changes with updating ComponentConfig
     */
    applyChanges() {
        if (this.edge != null) {
            if (this.edge.roleIsAtLeast('owner')) {

                /** fill udateComponentArray with changed formgroup.controls */
                let updateComponentArray = [];
                Object.keys(this.formGroup.controls).forEach((element, index) => {
                    if (this.formGroup.controls[element].dirty) {
                        updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value })
                    }
                })

                /** Update component.properties */
                this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray).then(() => {

                    /** set Components-properties-value to FormGroup-value   */
                    for (let i = 0; i < updateComponentArray.length; i++) {
                        this.component.properties[updateComponentArray[i].name] = this.formGroup.controls[updateComponentArray[i].name].value
                    }
                    this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
                }).catch(reason => {

                    /** set Formgroup-value to Components-properties-value */
                    for (let i = 0; i < updateComponentArray.length; i++) {
                        this.formGroup.controls[updateComponentArray[i].name].setValue(this.component.properties[updateComponentArray[i].name])
                    }
                    this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
                    console.warn(reason);
                })
                this.formGroup.markAsPristine()
            } else {
                this.service.toast(this.translate.instant('General.insufficientRights'), 'danger');
            }
        }
    }

    /**
     * Called on every new data.
     * 
     * @param currentData new data for the subscribed Channel-Addresses
     */
    protected onCurrentData(currentData: CurrentData) {
    }

    /**
     * Gets the ChannelAddresses that should be subscribed.
     */
    protected getChannelAddresses(): ChannelAddress[] {
        return [];
    }

    /**
     * Gets the ChannelIds of the current Component that should be subscribed.
     */
    protected getChannelIds(): string[] {
        return [];
    }

    /** Gets the FormGroup of the current Component */
    protected getFormGroup(): FormGroup {
        return
    }
}