import { Component, Input } from "@angular/core";
import { Edge, EdgeConfig } from "src/app/shared/shared";
import { Icon } from "src/app/shared/type/widget";
import { AbstractModal } from "./abstractModal";

@Component({
    selector: 'oe-modal',
    templateUrl: 'modal.html',
})
export class ModalComponent extends AbstractModal {
    @Input() title: string;
    @Input() icon: Icon;
    @Input() component: EdgeConfig.Component;

    public edge: Edge = null;
    public config: EdgeConfig = null;

    // constructor(
    //     public service: Service,
    //     public translate: TranslateService,
    //     public modalCtrl: ModalController,
    //     public websocket: Websocket,
    //     public route: ActivatedRoute,
    // ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.service.getConfig().then(config => {
                // store important variables publically
                this.edge = edge;
                this.config = config;
                this.component = config.components[this.componentId];
            })
        })
    }

    // public modalCtrl: ModalController,
    // public service: Service,
    // public formBuilder: FormBuilder,
    // public websocket: Websocket,
    // public translate: TranslateService

}