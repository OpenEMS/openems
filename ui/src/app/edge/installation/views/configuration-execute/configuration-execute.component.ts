// @ts-strict-ignore
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { Edge, Service, Websocket } from 'src/app/shared/shared';
import { AbstractIbn } from '../../installation-systems/abstract-ibn';
import { IbnUtils } from '../../shared/ibnutils';
import { ComponentConfigurator, ConfigurationObject, ConfigurationState, FunctionState } from './component-configurator';

@Component({
  selector: ConfigurationExecuteComponent.SELECTOR,
  templateUrl: './configuration-execute.component.html',
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

  constructor(
    private service: Service,
    private websocket: Websocket,
    private translate: TranslateService,
  ) { }

  public ngOnInit() {
    const stopOnRequest: Subject<void> = new Subject<void>();

    this.edge.getConfig(this.websocket).pipe(
      takeUntil(stopOnRequest),
      filter(config => config !== null),
    ).subscribe((config) => {
      stopOnRequest.next();
      stopOnRequest.complete();

      // Add objects to component configurator
      this.componentConfigurator = this.ibn.getComponentConfigurator(this.edge, config, this.websocket, this.service);
      this.configurationObjectsToBeConfigured = this.componentConfigurator.getConfigurationObjectsToBeConfigured();
      this.isAnyConfigurationObjectPreConfigured = this.componentConfigurator.anyHasConfigurationState(ConfigurationState.PreConfigured);

      // To update scheduler.
      this.ibn.setRequiredControllers();
      IbnUtils.addIbnToSessionStorage(this.ibn);

      // Auto-start configuration when no components pre-configured
      if (this.isAnyConfigurationObjectPreConfigured) {
        this.service.toast(this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.EXISTING_CONFIGURATION'), 'warning');
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
      this.service.toast(this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.SUCCESSFULLY_CONFIGURED'), 'success');
    }).catch((reason) => {
      console.log(reason);

      if (!this.componentConfigurator.allHaveConfigurationState(ConfigurationState.Configured)) {
        this.service.toast(this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.PARTIAL_CONFIGURATION'), 'danger');
        return;
      }

      if (!this.componentConfigurator.allHaveFunctionState(FunctionState.Ok)) {
        this.service.toast(this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.FUNCTION_TEST_ERROR'), 'warning');
        return;
      }
    }).finally(() => {
      this.service.toast(this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.START_FUNCTION_TEST'), 'warning', 5000);
      this.componentConfigurator.startFunctionTest().then(() => {
        this.service.toast(this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.FINISH_FUNCTION_TEST'), 'success');
        this.isWaiting = false;
      });
    });
  }
}
