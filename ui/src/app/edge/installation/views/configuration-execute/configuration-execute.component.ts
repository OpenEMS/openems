import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { Edge, Service, Websocket } from 'src/app/shared/shared';
import { environment } from 'src/environments';
import { AbstractIbn } from '../../installation-systems/abstract-ibn';
import { ComponentConfigurator, ConfigurationObject, ConfigurationState, FunctionState } from './component-configurator';

@Component({
  selector: ConfigurationExecuteComponent.SELECTOR,
  templateUrl: './configuration-execute.component.html'
})
export class ConfigurationExecuteComponent implements OnInit {

  private static readonly SELECTOR = 'configuration-execute';

  @Input() public ibn: AbstractIbn;
  @Input() public edge: Edge;
  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent: EventEmitter<any> = new EventEmitter();

  public isWaiting = false;
  public componentConfigurator: ComponentConfigurator;
  public configurationObjectsToBeConfigured: ConfigurationObject[];
  public isAnyConfigurationObjectPreConfigured: boolean;

  constructor(private service: Service, private websocket: Websocket) { }

  public ngOnInit() {
    const stopOnRequest: Subject<void> = new Subject<void>();

    this.edge.getConfig(this.websocket).pipe(
      takeUntil(stopOnRequest),
      filter(config => config !== null)
    ).subscribe((config) => {
      stopOnRequest.next();
      stopOnRequest.complete();

      // Add objects to component configurator
      this.componentConfigurator = this.ibn.getComponentConfigurator(this.edge, config, this.websocket, this.service);

      this.configurationObjectsToBeConfigured = this.componentConfigurator.getConfigurationObjectsToBeConfigured();
      this.isAnyConfigurationObjectPreConfigured = this.componentConfigurator.anyHasConfigurationState(ConfigurationState.PreConfigured);

      // To update scheduler.
      this.ibn.setRequiredControllers();
      sessionStorage.setItem('ibn', JSON.stringify(this.ibn));

      // Auto-start configuration when no components pre-configured
      if (this.isAnyConfigurationObjectPreConfigured) {
        this.service.toast('Es wurden eine bestehende Konfiguration gefunden.'
          + 'Sie können diese überschreiben, indem Sie den Konfigurationsvorgang manuell starten.', 'warning');
      } else {
        this.startConfiguration();
      }
    });
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {
    this.nextViewEvent.emit();
  }

  /**
   * Starts the configuration based on the components selected.
   */
  public startConfiguration() {
    this.isWaiting = true;

    // Starts the configuration
    this.componentConfigurator.start().then(() => {
      this.service.toast('Konfiguration erfolgreich.', 'success');
    }).catch((reason) => {
      console.log(reason);

      if (!this.componentConfigurator.allHaveConfigurationState(ConfigurationState.Configured)) {
        this.service.toast('Es konnten nicht alle Komponenten richtig konfiguriert werden.', 'danger');
        return;
      }

      if (!this.componentConfigurator.allHaveFunctionState(FunctionState.Ok)) {
        this.service.toast('Funktionstest mit Fehlern abgeschlossen.', 'warning');
        return;
      }
    }).finally(() => {
      this.isWaiting = false;
    });
  }
}
