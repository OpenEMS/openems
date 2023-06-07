import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { environment } from 'src/environments';
import { Edge, Service, Utils, Websocket } from '../../../shared/shared';

@Component({
  selector: SystemUpdateComponent.SELECTOR,
  templateUrl: './systemupdate.component.html'
})
export class SystemUpdateComponent implements OnInit {

  private static readonly SELECTOR = "systemUpdate";

  public readonly environment = environment;
  public readonly spinnerId: string = SystemUpdateComponent.SELECTOR;
  public showLog: boolean = false;
  public readonly ESTIMATED_REBOOT_TIME = 600; // Seconds till the openems service is restarted after update

  public edge: Edge;
  public restartTime: number = this.ESTIMATED_REBOOT_TIME;

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private service: Service
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent("", this.route).then(edge => {
      this.edge = edge;
    });
  }

}
