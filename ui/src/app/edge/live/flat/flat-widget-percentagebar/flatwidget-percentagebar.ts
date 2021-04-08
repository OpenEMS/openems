import { Component, Input } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { UUID } from "angular2-uuid";
import { Service, Websocket } from "src/app/shared/shared";
import { ChannelAddress } from "src/app/shared/type/channeladdress";
import { AbstractFlatWidgetComponent } from "../../abstractFlatWidget.component";


@Component({
    selector: 'flat-widget-percentagebar',
    templateUrl: './flatwidget-percentagebar.html'
})


export class FlatWidgetPercentagebar extends AbstractFlatWidgetComponent {


    /** value is the channel the percentagebar is refering to */
    @Input() value: string = null;
    public isChannelValue: boolean = false;
    constructor(
        public route: ActivatedRoute,
        public service: Service,
        public websocket: Websocket,
    ) {
        super(websocket);
    }
    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        });
        if (typeof (this.value) === 'string') {
            this.isChannelValue = true;
            this.subscribeOnChannels(UUID.UUID().toString(), [ChannelAddress.fromString(this.value)]);
        }

    }
}