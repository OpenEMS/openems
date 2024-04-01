import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { Edge, Service, Websocket } from 'src/app/shared/shared';
import { environment } from 'src/environments';
import { ExecuteSystemUpdate } from './executeSystemUpdate';
import { SystemUpdateState } from './getSystemUpdateStateResponse';

@Component({
  selector: ExecuteSystemUpdateComponent.SELECTOR,
  templateUrl: './executesystemupdate.component.html',
})
export class ExecuteSystemUpdateComponent implements OnInit, OnDestroy {

  private static readonly SELECTOR = "executesystemupdate";
  public readonly spinnerId: string = ExecuteSystemUpdateComponent.SELECTOR;

  @Input() public executeUpdateInstantly: boolean = false;
  @Input() public edge: Edge;
  public readonly environment = environment;
  protected executeUpdate: ExecuteSystemUpdate = null;

  protected isWaiting: boolean;

  @Output() public stateChanged: EventEmitter<SystemUpdateState> = new EventEmitter();

  constructor(
    private websocket: Websocket,
    private service: Service) { }

  ngOnInit() {
    this.executeUpdate = new ExecuteSystemUpdate(this.edge, this.websocket);

    this.executeUpdate.systemUpdateStateChange = (systemUpdateState) => {
      this.stateChanged.emit(systemUpdateState);
      if (systemUpdateState.updated) {
        this.service.stopSpinner(this.spinnerId);
        this.isWaiting = false;
      }
    };

    this.service.startSpinnerTransparentBackground(this.spinnerId);
    this.isWaiting = true;
    this.executeUpdate.start()
      .finally(() => {
        if (!this.executeUpdate.systemUpdateState.running) {
          this.service.stopSpinner(this.spinnerId);
          this.isWaiting = false;
        }
        if (this.executeUpdate.systemUpdateState.available && this.executeUpdateInstantly) {
          this.executeSystemUpdate();
        }
      });
  }

  public ngOnDestroy() {
    this.executeUpdate.stop();
  }

  public executeSystemUpdate() {
    this.service.startSpinnerTransparentBackground(this.spinnerId);
    this.isWaiting = true;
    this.executeUpdate.executeSystemUpdate();
  }

}
