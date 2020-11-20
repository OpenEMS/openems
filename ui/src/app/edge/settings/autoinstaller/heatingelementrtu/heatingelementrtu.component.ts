import { Component } from '@angular/core';
import { Edge, EdgeConfig, Service, ChannelAddress, Websocket, Utils } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';
import { Subject, BehaviorSubject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { isUndefined } from 'util';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: HeatingElementRtuInstallerComponent.SELECTOR,
  templateUrl: './heatingelementrtu.component.html'
})
export class HeatingElementRtuInstallerComponent {

  public checkingState: boolean = false;
  public loading = true;
  public running = false;
  public showInit: boolean = false;
  public appWorking: BehaviorSubject<boolean> = new BehaviorSubject(false);
  public progressPercentage: number = 0;

  public loadingStrings: { string: string, type: string }[] = [];
  public subscribedChannels: ChannelAddress[] = [];
  public components: EdgeConfig.Component[] = [];

  public heatingElementId = null;
  public edge: Edge = null;
  public config: EdgeConfig = null;
  private stopOnDestroy: Subject<void> = new Subject<void>();

  private static readonly SELECTOR = "heatingElementRtuInstaller";

  constructor(
    private route: ActivatedRoute,
    private websocket: Websocket,
    public modalCtrl: ModalController,
    public service: Service,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('Automatische Installation', this.route).then(edge => {
      this.edge = edge;
    });
    this.service.getConfig().then(config => {
      this.config = config;
    }).then(() => {
      switch (this.gatherAddedComponents().length) {
        case 0: {
          this.showInit = true;
          this.loading = false;
          break;
        }
        case 1: case 2: {
          this.loadingStrings.push({ string: 'Teile dieser App sind bereits installiert', type: 'setup' });
          setTimeout(() => {
            this.addHeatingElementComponents();
          }, 2000);
          break;
        }
        case 3: {
          this.gatherAddedComponentsIntoArray();
          this.checkingState = true;
          break;
        }
      }
    });
  }

  // used to assemble properties out of created fields and model from 'gather' methods
  private createProperties(fields: FormlyFieldConfig[], model): { name: string, value: any }[] {
    let result: { name: string, value: any }[] = [];
    fields.forEach(field => {
      if (field.key == 'alias') {
        result.push({ name: 'alias', value: 'Heizstab' })
      }
      Object.keys(model).forEach(modelKey => {
        if (field.key == modelKey) {
          result.push({ name: field.key.toString(), value: model[modelKey] })
        }
      })
    })
    return result;
  }

  private gatherType(config: EdgeConfig): { name: string, value: any }[] {
    let factoryId = 'IO.KMtronic'
    let factory = config.factories[factoryId];
    let fields: FormlyFieldConfig[] = [];
    let model = {};
    for (let property of factory.properties) {
      let property_id = property.id.replace('.', '_');
      let field: FormlyFieldConfig = {
        key: property_id,
        type: 'input',
        templateOptions: {
          label: property.name,
          description: property.description
        }
      }
      // add Property Schema 
      Utils.deepCopy(property.schema, field);
      fields.push(field);
      if (property.defaultValue != null) {
        model[property_id] = property.defaultValue;

        if (property.name == 'Component-ID') {
          model[property_id] = 'io1';
        }
        // set costum modbus-id
        if (property.name == 'Modbus-ID') {
          model[property_id] = 'modbus20';
        }
      }
    }
    let properties = this.createProperties(fields, model);
    return properties;
  }

  private gatherApp(config: EdgeConfig): { name: string, value: any }[] {
    let factoryId = 'Controller.IO.HeatingElement';
    let factory = config.factories[factoryId];
    let fields: FormlyFieldConfig[] = [];
    let model = {};
    for (let property of factory.properties) {
      let property_id = property.id.replace('.', '_');
      let field: FormlyFieldConfig = {
        key: property_id,
        type: 'input',
        templateOptions: {
          label: property.name,
          description: property.description
        }
      }
      // add Property Schema 
      Utils.deepCopy(property.schema, field);
      fields.push(field);
      if (property.defaultValue != null) {
        model[property_id] = property.defaultValue;

        if (property.name == 'Component-ID') {
          model[property_id] = 'ctrlIoHeatingElement1';
        }
        if (property.name == 'Mode') {
          model[property_id] = 'MANUAL_OFF';
        }
        if (property.name == 'Output Channel Phase L1') {
          model[property_id] = "io1/Relay4";
        }
        if (property.name == 'Output Channel Phase L2') {
          model[property_id] = "io1/Relay5";
        }
        if (property.name == 'Output Channel Phase L3') {
          model[property_id] = "io1/Relay6";
        }
      }
    }
    let properties = this.createProperties(fields, model);
    return properties;
  }

  private gatherCommunication(config: EdgeConfig): { name: string, value: any }[] {
    let factoryId = 'Bridge.Modbus.Serial';
    let factory = config.factories[factoryId];
    let fields: FormlyFieldConfig[] = [];
    let model = {};
    for (let property of factory.properties) {
      let property_id = property.id.replace('.', '_');
      let field: FormlyFieldConfig = {
        key: property_id,
        type: 'input',
        templateOptions: {
          label: property.name,
          description: property.description
        }
      }
      // add Property Schema 
      Utils.deepCopy(property.schema, field);
      fields.push(field);
      if (property.defaultValue != null) {
        model[property_id] = property.defaultValue;
        // set costum component id
        if (property.name == 'Component-ID') {
          model[property_id] = 'modbus20';
        }
      }
    }
    let properties = this.createProperties(fields, model);
    return properties;
  }

  /**
   * Main method, to add all required components.
   */
  public addHeatingElementComponents() {

    let addedComponents: number = 0;

    this.loading = true;
    this.showInit = false;

    // Adding modbus rtu bridge 
    this.loadingStrings.push({ string: 'Versuche Bridge.Modbus.Serial (RTU) hinzuzufügen..', type: 'setup' });
    this.edge.createComponentConfig(this.websocket, 'Bridge.Modbus.Serial', this.gatherCommunication(this.config)).then(() => {
      setTimeout(() => {
        this.loadingStrings.push({ string: 'Bridge.Modbus.Serial wurde hinzugefügt', type: 'success' });
        addedComponents += 1;
      }, 2000);
    }).catch(reason => {
      if (reason.error.code == 1) {
        setTimeout(() => {
          this.loadingStrings.push({ string: 'Bridge.Modbus.Serial existiert bereits', type: 'success' });
          addedComponents += 1;
        }, 2000);
        return;
      }
      setTimeout(() => {
        this.loadingStrings.push({ string: 'Fehler Bridge.Modbus.Serial hinzuzufügen', type: 'danger' });
        addedComponents += 1;
      }, 2000);
    });

    // Adding kmtronic relay board
    setTimeout(() => {
      this.loadingStrings.push({ string: 'Versuche IO.KMtronic hinzuzufügen..', type: 'setup' });
      this.edge.createComponentConfig(this.websocket, 'IO.KMtronic', this.gatherType(this.config)).then(() => {
        setTimeout(() => {
          this.loadingStrings.push({ string: 'IO.KMtronic wurde hinzugefügt', type: 'success' });
          addedComponents += 1;
        }, 1000);
      }).catch(reason => {
        if (reason.error.code == 1) {
          setTimeout(() => {
            this.loadingStrings.push({ string: 'IO.KMtronic existiert bereits', type: 'success' });
            addedComponents += 1;
          }, 1000);
          return;
        }
        setTimeout(() => {
          this.loadingStrings.push({ string: 'Fehler IO.KMtronic hinzuzufügen', type: 'danger' });
          addedComponents += 1;
        }, 1000);
      });
    }, 3000);

    // Adding heating element controller
    setTimeout(() => {
      this.loadingStrings.push({ string: 'Versuche Controller.IO.HeatingElement hinzuzufügen..', type: 'setup' });
      this.edge.createComponentConfig(this.websocket, 'Controller.IO.HeatingElement', this.gatherApp(this.config)).then(() => {
        setTimeout(() => {
          this.loadingStrings.push({ string: 'Controller.IO.HeatingElement wurde hinzugefügt', type: 'success' });
          addedComponents += 1;
        }, 1000);
      }).catch(reason => {
        if (reason.error.code == 1) {
          setTimeout(() => {
            this.loadingStrings.push({ string: 'Controller.IO.HeatingElement existiert bereits', type: 'success' });
            addedComponents += 1;
          }, 1000);
          return;
        }
        setTimeout(() => {
          this.loadingStrings.push({ string: 'Fehler Controller.IO.HeatingElement hinzuzufügen', type: 'danger' });
          addedComponents += 1;
        }, 1000);
      });
    }, 5000);

    var percentageInterval = setInterval(() => {
      while (addedComponents == 3) {
        this.progressPercentage = 0.4;
        clearInterval(percentageInterval);
        break;
      }
    }, 300)

    setTimeout(() => {
      this.checkConfiguration();
    }, 8000);
  }

  private gatherAddedComponents(): EdgeConfig.Component[] {
    let result = [];
    this.config.getComponentsByFactory('Bridge.Modbus.Serial').forEach(component => {
      if (component.id == 'modbus20') {
        result.push(component)
      }
    })
    this.config.getComponentsByFactory('IO.KMtronic').forEach(component => {
      if (component.properties['modbus.id'] == 'modbus20') {
        result.push(component)
      }
    })
    this.config.getComponentsByFactory('Controller.IO.HeatingElement').forEach(component => {
      if (component.id == 'ctrlIoHeatingElement1') {
        result.push(component)
      }
    })
    return result
  }

  private gatherAddedComponentsIntoArray() {
    this.config.getComponentsByFactory('Bridge.Modbus.Serial').forEach(component => {
      if (component.id == 'modbus20') {
        this.components.push(component)
      }
    })
    this.config.getComponentsByFactory('IO.KMtronic').forEach(component => {
      if (component.properties['modbus.id'] == 'modbus20') {
        this.components.push(component)
      }
    })
    this.config.getComponentsByFactory('Controller.IO.HeatingElement').forEach(component => {
      if (component.id == 'ctrlIoHeatingElement1') {
        this.heatingElementId = component.id;
        this.components.push(component)
      }
    })
    this.edge.currentData.pipe(takeUntil(this.stopOnDestroy)).subscribe(currentData => {
      let workState = 0;
      this.components.forEach(component => {
        let state = currentData.channel[component.id + '/State'];
        if (!isUndefined(state)) {
          if (state == 0) {
            workState += 1;
          }
        }
      })
      if (workState == 3) {
        this.appWorking.next(true);
      } else {
        this.appWorking.next(false);
      }
    })
    this.subscribeOnAddedComponents();
  }

  private checkConfiguration() {
    this.loadingStrings = [];
    this.loadingStrings.push({ string: 'Überprüfe ob Komponenten korrekt hinzugefügt wurden..', type: 'setup' });
    this.progressPercentage = 0.6;
    setTimeout(() => {
      this.service.getConfig().then(config => {
        this.config = config;
      }).then(() => {
        //TODO: Check it properly
        if (this.gatherAddedComponents().length >= 3) {
          this.loadingStrings.push({ string: 'Komponenten korrekt hinzugefügt', type: 'success' });
          this.progressPercentage = 0.95;
          this.gatherAddedComponentsIntoArray();
          return
        }
        this.loadingStrings.push({ string: 'Es konnten nicht alle Komponenten korrekt hinzugefügt werden', type: 'danger' });
        this.loadingStrings.push({ string: 'Bitte Prozess neu starten oder gegebenenfalls manuell korrigieren', type: 'danger' });
      })
    }, 10000);
  }

  private subscribeOnAddedComponents() {
    this.loadingStrings.push({ string: 'Überprüfe Status der Komponenten..', type: 'setup' });
    this.components.forEach(component => {
      this.subscribedChannels.push(new ChannelAddress(component.id, 'State'));
      Object.keys(component.channels).forEach(channel => {
        if (component.channels[channel]['level']) {
          let levelChannel = new ChannelAddress(component.id, channel);
          this.subscribedChannels.push(levelChannel)
        }
      });
    })
    this.edge.subscribeChannels(this.websocket, 'heatingElementRtuInstaller', this.subscribedChannels);
    setTimeout(() => {
      this.loading = false;
      this.running = true;
    }, 5000);
  }

  ionViewDidLeave() {
    this.edge.unsubscribeChannels(this.websocket, 'heatingElementRtuInstaller');
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }
}