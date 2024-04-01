import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { environment } from 'src/environments';
import { Edge, Service, UserPermission, Utils } from '../../../shared/shared';

@Component({
  selector: SystemComponent.SELECTOR,
  templateUrl: './system.component.html',
})
export class SystemComponent implements OnInit {

  private static readonly SELECTOR = "system";

  public readonly environment = environment;
  public readonly spinnerId: string = SystemComponent.SELECTOR;
  public showLog: boolean = false;
  public readonly ESTIMATED_REBOOT_TIME = 600; // Seconds till the openems service is restarted after update

  public edge: Edge;
  public restartTime: number = this.ESTIMATED_REBOOT_TIME;

  protected canSeeSystemRestart: boolean = false;

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private service: Service,
  ) { }

  ngOnInit() {
    this.service.getCurrentEdge().then(edge => {
      this.edge = edge;
      this.canSeeSystemRestart = UserPermission.isAllowedToSeeSystemRestart(this.service.currentUser, edge);
    });
  }
}
