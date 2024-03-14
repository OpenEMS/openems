import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { SystemUpdateState } from 'src/app/edge/settings/system/getSystemUpdateStateResponse';
import { Edge, Service, Websocket } from 'src/app/shared/shared';

@Component({
  selector: PreInstallationUpdateComponent.SELECTOR,
  templateUrl: './pre-installation-update.component.html',
})
export class PreInstallationUpdateComponent {
  private static readonly SELECTOR = 'pre-installation-update';

  public readonly spinnerId: string = PreInstallationUpdateComponent.SELECTOR;

  @Output() public nextViewEvent = new EventEmitter();
  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();

  @Input() public edge: Edge;

  protected isWaiting: boolean;
  protected isUpdated: boolean;

  constructor(private service: Service, public websocket: Websocket, private translate: TranslateService) { }

  public onNextClicked() {
    if (!this.isUpdated) {
      this.service.toast(this.translate.instant('INSTALLATION.SYSTEM_UPDATE.UPDATE_WARNING'), "danger");
      return;
    }
    this.nextViewEvent.emit();
  }

  public updateStateChanged(updateState: SystemUpdateState) {
    if (updateState.running) {
      this.isWaiting = true;
      return;
    } else if (updateState.updated) {
      this.isUpdated = true;
    }
    this.isWaiting = false;
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

}
