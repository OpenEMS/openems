import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { Ibn } from '../../installation-systems/abstract-ibn';

export enum FeedInSetting {
  Undefined = "UNDEFINED",
  QuEnableCurve = "QU_ENABLE_CURVE",
  PuEnableCurve = "PU_ENABLE_CURVE",
  FixedPowerFactor = "FIXED_POWER_FACTOR",
  // Lagging Power Factor
  Lagging_0_80 = "LAGGING_0_80",
  Lagging_0_81 = "LAGGING_0_81",
  Lagging_0_82 = "LAGGING_0_82",
  Lagging_0_83 = "LAGGING_0_83",
  Lagging_0_84 = "LAGGING_0_84",
  Lagging_0_85 = "LAGGING_0_85",
  Lagging_0_86 = "LAGGING_0_86",
  Lagging_0_87 = "LAGGING_0_87",
  Lagging_0_88 = "LAGGING_0_88",
  Lagging_0_89 = "LAGGING_0_89",
  Lagging_0_90 = "LAGGING_0_90",
  Lagging_0_91 = "LAGGING_0_91",
  Lagging_0_92 = "LAGGING_0_92",
  Lagging_0_93 = "LAGGING_0_93",
  Lagging_0_94 = "LAGGING_0_94",
  Lagging_0_95 = "LAGGING_0_95",
  Lagging_0_96 = "LAGGING_0_96",
  Lagging_0_97 = "LAGGING_0_97",
  Lagging_0_98 = "LAGGING_0_98",
  Lagging_0_99 = "LAGGING_0_99",
  // Leading Power Factor
  Leading_0_80 = "LEADING_0_80",
  Leading_0_81 = "LEADING_0_81",
  Leading_0_82 = "LEADING_0_82",
  Leading_0_83 = "LEADING_0_83",
  Leading_0_84 = "LEADING_0_84",
  Leading_0_85 = "LEADING_0_85",
  Leading_0_86 = "LEADING_0_86",
  Leading_0_87 = "LEADING_0_87",
  Leading_0_88 = "LEADING_0_88",
  Leading_0_89 = "LEADING_0_89",
  Leading_0_90 = "LEADING_0_90",
  Leading_0_91 = "LEADING_0_91",
  Leading_0_92 = "LEADING_0_92",
  Leading_0_93 = "LEADING_0_93",
  Leading_0_94 = "LEADING_0_94",
  Leading_0_95 = "LEADING_0_95",
  Leading_0_96 = "LEADING_0_96",
  Leading_0_97 = "LEADING_0_97",
  Leading_0_98 = "LEADING_0_98",
  Leading_0_99 = "LEADING_0_99",
  Leading_1 = "LEADING_1"
}

@Component({
  selector: ProtocolFeedInLimitation.SELECTOR,
  templateUrl: './protocol-feed-in-limitation.component.html'
})
export class ProtocolFeedInLimitation implements OnInit {

  private static readonly SELECTOR = "protocol-feed-in-limitation";

  @Input() public ibn: Ibn;
  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<Ibn>();
  @Output() public setIbnEvent = new EventEmitter<Ibn>();

  public form: FormGroup;
  public fields: FormlyFieldConfig[] = [];
  public model;
  public readonly FeedInType = FeedInType;
  private totalPvPower: number = 0;

  constructor() { }

  public ngOnInit() {
    this.form = new FormGroup({});
    let pv = this.ibn.pv;

    this.totalPvPower += (pv.dc1.isSelected ? pv.dc1.value : 0);
    this.totalPvPower += (pv.dc2.isSelected ? pv.dc2.value : 0);

    pv.ac.forEach(element => {
      this.totalPvPower += element.value ?? 0
    })

    this.getFields();

    this.model = this.ibn.feedInLimitation ??= {
      feedInType: FeedInType.DYNAMIC_LIMITATION,
      feedInSetting: FeedInSetting.QuEnableCurve,
      fixedPowerFactor: FeedInSetting.Undefined
    }
    this.model.maximumFeedInPower = this.totalPvPower;
    this.model.isManualProperlyFollowedAndRead = this.ibn.feedInLimitation.feedInType == FeedInType.EXTERNAL_LIMITATION;
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {
    if (this.form.invalid) {
      return;
    }

    this.ibn.feedInLimitation = {
      feedInType: this.model.feedInType,
      feedInSetting: this.model.feedInSetting ?? FeedInSetting.Undefined,
      fixedPowerFactor: this.model.fixedPowerFactor
    }

    if (this.model.feedInType == FeedInType.DYNAMIC_LIMITATION) {
      this.ibn.feedInLimitation.feedInSetting = this.model.feedInSetting ?? FeedInSetting.Undefined,
        this.ibn.feedInLimitation.fixedPowerFactor = this.model.fixedPowerFactor ?? FeedInSetting.Undefined,
        this.ibn.feedInLimitation.maximumFeedInPower = this.form.controls["maximumFeedInPower"]?.dirty ? this.form.controls["maximumFeedInPower"].value : this.totalPvPower;
    } else {
      this.ibn.feedInLimitation.maximumFeedInPower = this.form.controls["maximumFeedInPower"]?.dirty ? this.form.controls["maximumFeedInPower"].value : this.totalPvPower;
    }

    this.setIbnEvent.emit(this.ibn);
    this.nextViewEvent.emit();
  }

  public getFields(): void {

    this.fields.push({
      key: "feedInType",
      type: "select",
      className: "white-space-initial",
      templateOptions: {
        label: "Typ",
        placeholder: "Select Option",
        options: [
          { label: "Dynamische Begrenzung der Einspeisung (z.B. 70% Abregelung)", value: FeedInType.DYNAMIC_LIMITATION },
          { label: "Rundsteuerempfänger (Externe Abregelung durch Netzbetreiber)", value: FeedInType.EXTERNAL_LIMITATION }
        ],
        required: true,
      }
    })

    this.fields.push({
      key: "maximumFeedInPower",
      type: "input",
      templateOptions: {
        type: "number",
        label: "Maximale Einspeiseleistung [W]",
        description: "Diesen Wert entnehmen Sie der Anschlussbestätigung des Netzbetreibers",
        required: true
      },
      parsers: [Number],
      defaultValue: parseInt((this.totalPvPower * 0.7).toFixed(0)),
      hideExpression: model => model.feedInType != FeedInType.DYNAMIC_LIMITATION
    });

    this.fields.push({
      key: "feedInSetting",
      type: "radio",
      templateOptions: {
        label: "Typ",
        description: "Wirkleistungsreduzierung bei Überfrequenz",
        options: [
          { label: "Blindleistungs-Spannungskennlinie Q(U)", value: FeedInSetting.QuEnableCurve },
          { label: "Verschiebungsfaktor-/Wirkleistungskennlinie Cos φ (P)", value: FeedInSetting.PuEnableCurve },
          { label: "Fester Verschiebungsfaktor Cos φ", value: FeedInSetting.FixedPowerFactor }
        ],
        required: true
      },
      hideExpression: model => model.feedInType != FeedInType.DYNAMIC_LIMITATION
    });

    this.fields.push({
      key: "fixedPowerFactor",
      type: "select",
      templateOptions: {
        label: "Cos φ Festwert",
        options: [
          // Leading
          { label: "0.80", value: FeedInSetting.Leading_0_80 },
          { label: "0.81", value: FeedInSetting.Leading_0_81 },
          { label: "0.82", value: FeedInSetting.Leading_0_82 },
          { label: "0.83", value: FeedInSetting.Leading_0_83 },
          { label: "0.84", value: FeedInSetting.Leading_0_84 },
          { label: "0.85", value: FeedInSetting.Leading_0_85 },
          { label: "0.86", value: FeedInSetting.Leading_0_86 },
          { label: "0.87", value: FeedInSetting.Leading_0_87 },
          { label: "0.88", value: FeedInSetting.Leading_0_88 },
          { label: "0.89", value: FeedInSetting.Leading_0_89 },
          { label: "0.90", value: FeedInSetting.Leading_0_90 },
          { label: "0.91", value: FeedInSetting.Leading_0_91 },
          { label: "0.92", value: FeedInSetting.Leading_0_92 },
          { label: "0.93", value: FeedInSetting.Leading_0_93 },
          { label: "0.94", value: FeedInSetting.Leading_0_94 },
          { label: "0.95", value: FeedInSetting.Leading_0_95 },
          { label: "0.96", value: FeedInSetting.Leading_0_96 },
          { label: "0.97", value: FeedInSetting.Leading_0_97 },
          { label: "0.98", value: FeedInSetting.Leading_0_98 },
          { label: "0.99", value: FeedInSetting.Leading_0_99 },
          { label: "1", value: FeedInSetting.Leading_1 },
          // Lagging
          { label: "-0.80", value: FeedInSetting.Lagging_0_80 },
          { label: "-0.81", value: FeedInSetting.Lagging_0_81 },
          { label: "-0.82", value: FeedInSetting.Lagging_0_82 },
          { label: "-0.83", value: FeedInSetting.Lagging_0_83 },
          { label: "-0.84", value: FeedInSetting.Lagging_0_84 },
          { label: "-0.85", value: FeedInSetting.Lagging_0_85 },
          { label: "-0.86", value: FeedInSetting.Lagging_0_86 },
          { label: "-0.87", value: FeedInSetting.Lagging_0_87 },
          { label: "-0.88", value: FeedInSetting.Lagging_0_88 },
          { label: "-0.89", value: FeedInSetting.Lagging_0_89 },
          { label: "-0.90", value: FeedInSetting.Lagging_0_90 },
          { label: "-0.91", value: FeedInSetting.Lagging_0_91 },
          { label: "-0.92", value: FeedInSetting.Lagging_0_92 },
          { label: "-0.93", value: FeedInSetting.Lagging_0_93 },
          { label: "-0.94", value: FeedInSetting.Lagging_0_94 },
          { label: "-0.95", value: FeedInSetting.Lagging_0_95 },
          { label: "-0.96", value: FeedInSetting.Lagging_0_96 },
          { label: "-0.97", value: FeedInSetting.Lagging_0_97 },
          { label: "-0.98", value: FeedInSetting.Lagging_0_98 },
          { label: "-0.99", value: FeedInSetting.Lagging_0_99 }
        ],
        required: true
      },
      hideExpression: model => model.feedInSetting !== FeedInSetting.FixedPowerFactor
    });

    this.fields.push({
      key: "isManualProperlyFollowedAndRead",
      type: "checkbox",
      templateOptions: {
        label: "Der Rundsteuerempfänger wurde lt. Anleitung ordnungsgemäß und vollständig installiert.",
        required: true
      },
      hideExpression: model => model.feedInType != FeedInType.EXTERNAL_LIMITATION
    });
  }

  openManual() {
    window.open("https://fenecon.de/download/16-anleitung-rundsteuerempfaenger-fenecon-home/")
  }
}

export enum FeedInType {
  DYNAMIC_LIMITATION,
  EXTERNAL_LIMITATION
}
