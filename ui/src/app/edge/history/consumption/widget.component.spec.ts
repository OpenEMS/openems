import {ComponentFixture, TestBed} from "@angular/core/testing";
import {ConsumptionComponent} from "./widget.component";
import {Service} from "../../../shared/service/service";
import {Edge} from "../../../shared/edge/edge";
import {Role} from "../../../shared/type/role";
import {ActivatedRoute} from "@angular/router";
import {By} from "@angular/platform-browser";
import {DebugElement} from "@angular/core";
import {EdgeConfig} from "../../../shared/edge/edgeconfig";
import ComponentChannel = EdgeConfig.ComponentChannel;
import {DefaultTypes} from "../../../shared/service/defaulttypes";
import {
  QueryHistoricTimeseriesEnergyResponse
} from "../../../shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import {PipeModule} from "../../../shared/pipe/pipe";

describe('history/ConsumptionComponent widget', () => {
  let component: ConsumptionComponent;
  let fixture: ComponentFixture<ConsumptionComponent>;

  const consumptionMeterFactory = new EdgeConfig.Factory(
    "Meter Microcare SDM 630", "Implements the Microcare SDM630 meter.",
    ["io.openems.edge.meter.api.SymmetricMeter"]
  );

  const meterFactoryId = 'Meter.Microcare.SDM630';

  const consumerActiveConsumptionEnergy: ComponentChannel = {
    accessMode: "RO",
    level: "INFO",
    type: "FLOAT",
    category: "OPENEMS_TYPE",
    unit: "kWh"
  };
  const consumptionMeter = new EdgeConfig.Component(
    meterFactoryId, {
      'type': 'CONSUMPTION_METERED'
    }, {
      'ActiveConsumptionEnergy': consumerActiveConsumptionEnergy
    }
  );
  consumptionMeter.alias="Heatpump Consumer"
  consumptionMeter.isEnabled = true;

  const edge = new Edge("edge0", "comment", "producttype", "1234.56.78", Role.ADMIN, true, new Date());

  // @ts-ignore
  const edgeConfig = new EdgeConfig(edge, {
    components: {
      'consumptionMeter': consumptionMeter
    },
    factories: {
      [meterFactoryId]: consumptionMeterFactory
    }
  })

  const queryHistoricTimeseriesEnergyResponse: QueryHistoricTimeseriesEnergyResponse = {
    id: 'random',
    jsonrpc: '2.0',
    result: {
      data: {
        'consumptionMeter/ActiveConsumptionEnergy': 2323.46
      }
    }
  };

  beforeEach(async () => {
    const service = jasmine.createSpyObj('Service', ['setCurrentComponent', 'getConfig', 'queryEnergy']);
    service.setCurrentComponent.and.returnValue(Promise.resolve(edge));
    service.getConfig.and.returnValue(Promise.resolve(edgeConfig))
    service.queryEnergy.and.returnValue(Promise.resolve(queryHistoricTimeseriesEnergyResponse))

    TestBed.configureTestingModule({
      declarations: [ConsumptionComponent],
      providers: [
        {provide: Service, useValue: service},
        {provide: ActivatedRoute, useValue: {}}
      ],
      imports: [PipeModule]
    });
  });

  it('should display consumption meter entries on period change', async () => {
    fixture = TestBed.createComponent(ConsumptionComponent);
    component = fixture.componentInstance;
    await component.ngOnInit();

    component.period = new DefaultTypes.HistoryPeriod();
    await component.ngOnChanges();
    fixture.detectChanges();

    await fixture.whenStable();
    const del: DebugElement = fixture.debugElement;
    const consumptionMeterEntry = del.query(
      By.css('[data-test-id="consumption-meter-component-entry"]')
    );
    expect(consumptionMeterEntry).toBeTruthy();
    expect(consumptionMeterEntry.nativeElement.textContent.trim()).toContain("Heatpump Consumer 2.3" + '\u00A0' + "kWh");
  });
});
