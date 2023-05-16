import { ChangeDetectorRef, Directive, Inject, Input, OnDestroy, OnInit } from "@angular/core";
import { FormBuilder, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, Utils, Websocket } from "src/app/shared/shared";
import { v4 as uuidv4 } from 'uuid';
import { Role } from "../../type/role";
import { TextIndentation } from "./modal-line/modal-line";

@Directive()
export abstract class AbstractModal implements OnInit, OnDestroy {

    @Input() component: EdgeConfig.Component = null;

    public isInitialized: boolean = false;
    public edge: Edge = null;
    public config: EdgeConfig = null;
    public stopOnDestroy: Subject<void> = new Subject<void>();
    public formGroup: FormGroup | null = null;

    /** Enum for User Role */
    public readonly Role = Role;

    /** Enum for Indentation */
    public readonly TextIndentation = TextIndentation;

    public readonly Utils = Utils;

    private selector: string = uuidv4();

    constructor(
        @Inject(Websocket) protected websocket: Websocket,
        @Inject(ActivatedRoute) protected route: ActivatedRoute,
        @Inject(Service) protected service: Service,
        @Inject(ModalController) public modalController: ModalController,
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
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.service.getConfig().then(config => {

                // store important variables publically
                this.edge = edge;
                this.config = config;

                // If component is passed
                let channelAddresses: ChannelAddress[] = [];

                // get the channel addresses that should be subscribed
                channelAddresses = this.getChannelAddresses();
                if (this.component != null) {
                    this.component = config.components[this.component.id];

                    let channelIds = this.getChannelIds();
                    for (let channelId of channelIds) {
                        channelAddresses.push(new ChannelAddress(this.component.id, channelId));
                    }
                }
                if (channelAddresses.length != 0) {
                    this.edge.subscribeChannels(this.websocket, this.selector, channelAddresses);
                }

                // call onCurrentData() with latest data
                edge.currentData.pipe(takeUntil(this.stopOnDestroy)).subscribe(currentData => {
                    let allComponents = {};
                    for (let channelAddress of channelAddresses) {
                        let ca = channelAddress.toString();
                        allComponents[ca] = currentData.channel[ca];
                    }
                    this.onCurrentData({ allComponents: allComponents });
                });
                this.formGroup = this.getFormGroup();

                // announce initialized
                this.isInitialized = true;
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
    protected getFormGroup(): FormGroup | null {
        return null
    }
}
