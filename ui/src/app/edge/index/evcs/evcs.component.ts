import { Component, Input } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge } from '../../../shared/edge/edge';
import { Service } from '../../../shared/service/service';
import { Websocket } from '../../../shared/service/websocket';
import { ChannelAddress } from '../../../shared/type/channeladdress';

@Component({
  selector: 'evcs',
  templateUrl: './evcs.component.html'
})
export class EvcsComponent {

  private static readonly SELECTOR = "evcs";

  @Input() private componentId: string;

  public edge: Edge = null;

  constructor(
    private service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute
  ) { }

  ngOnInit() {
    this.service.setCurrentEdge(this.route).then(edge => {
      this.edge = edge;
      edge.subscribeChannels(this.websocket, EvcsComponent.SELECTOR, [
        // Ess
        new ChannelAddress(this.componentId, 'ChargePower')
      ]);
    });
  }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, EvcsComponent.SELECTOR);
    }
  }

  // @ViewChildren(ChannelComponent)
  // private channelComponentChildren: QueryList<ChannelComponent>;
  // private stopOnDestroy: Subject<void> = new Subject<void>();
  // private formInitialized: boolean = false;

  // ngAfterViewChecked() {
  //   // unfortunately components are not available yet in ngAfterViewInit, so we need to call it again and again, till they are there.
  //   if (this.formInitialized || this.channelComponentChildren.length == 0) {
  //     return;
  //   }
  //   this.channelComponentChildren.forEach(channelComponent => {
  //     channelComponent.message
  //       .pipe(takeUntil(this.stopOnDestroy))
  //       .subscribe((message) => {
  //         if (message != null) {
  //           this.edge.send(message);
  //         }
  //       });
  //   });
  //   this.formInitialized = true;
  // }
}
