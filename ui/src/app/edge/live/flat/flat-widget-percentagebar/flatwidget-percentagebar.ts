import { Component, Input } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { Service, Websocket } from "src/app/shared/shared";

@Component({
    selector: 'flat-widget-percentagebar',
    templateUrl: './flatwidget-percentagebar.html'
})


export class FlatWidgetPercentagebar {


    /** value is the channel the percentagebar is refering to */
    @Input() value: string = null;
    public isChannelValue: boolean = false;
    constructor(
        public route: ActivatedRoute,
        public service: Service,
        public websocket: Websocket,
    ) {
        // super();
    }
    // ngOnInit() {
    //     this.service.setCurrentComponent('', this.route).then(edge => {
    //         this.edge = edge;
    //     });
    //     if (typeof (this.value) === 'string') {
    //         this.isChannelValue = true;
    //         this.subscribeOnChannels(UUID.UUID().toString(), [ChannelAddress.fromString(this.value)]);
    //     }

    // }
}