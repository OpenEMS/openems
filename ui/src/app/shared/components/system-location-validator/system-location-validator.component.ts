import { Component, EventEmitter, inject, Input, OnInit, Output } from "@angular/core";
import { FormGroup, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyFieldConfig, FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { environment } from "src/environments";
import { ComponentJsonApiRequest } from "../../jsonrpc/request/componentJsonApiRequest";
import { Service, Websocket } from "../../shared";
import { COUNTRY_OPTIONS, CountryUtils } from "../../type/country";
import { Language } from "../../type/language";
import { ObjectUtils } from "../../utils/object/object.utils";
import { PromiseUtils } from "../../utils/promise/promise.utils";
import { Edge } from "../edge/edge";
import { FormlyUtils } from "../formly/formly-utils";
import de from "./i18n/de.json";
import en from "./i18n/en.json";
import { GeoCodeJsonRpc } from "./json-rpc/geo-code-json-rpc";

export type ComponentData = {
    label: string;
    value: any;
    default?: boolean;
    imageUrl?: string;
    docUrl?: string;
};

export interface LocationModel {
    street: string;
    zip: string;
    city: string;
    country: string;
}

export type GeoResult = {
    country: string,
    countryCode: string,
    currency: string,
    houseNumber: string,
    latitude: string,
    longitude: string,
    openStreetMapUrl: string,
    placeName: string,
    postcode: string,
    road: string,
    subdivision: string,
    subdivisionCode: string,
    timezone: string,
};

export type ValidatorContext = "INSTALLATION" | "PROFILE";

export type SaveResult =
    | { status: "SUCCESS", geoResult: GeoResult }
    | { status: "UNKNOWN_ADDRESS_SELECTED" }
    | { status: "INVALID" };

@Component({
    selector: "system-location-validator",
    templateUrl: "./system-location-validator.component.html",
    standalone: true,
    imports: [
        IonicModule,
        ReactiveFormsModule,
        FormlyModule,
        TranslateModule,
    ],
})
export class SystemLocationValidatorComponent implements OnInit {
    @Input() public edge: Edge | null = null;

    @Input() public context: ValidatorContext = "PROFILE";

    @Output() public addressUpdated = new EventEmitter<void>();
    @Output() public isBusyChange = new EventEmitter<boolean>();

    protected form: FormGroup = new FormGroup({});
    protected fields: FormlyFieldConfig[] = [];
    protected model: LocationModel = { street: "", zip: "", city: "", country: "" };

    protected geoCodeForm: FormGroup = new FormGroup({});
    protected geoCodeFields: FormlyFieldConfig[] = [];
    protected geoCodeModel: { index: number | null } = { index: null };

    protected environment = environment;
    protected isValidating = false;
    protected isSaving: boolean = false;
    protected addressesAvailable = false;
    protected unknownAddressOptionIndex: number | null = null;
    protected id: number | null = null;
    protected isGeoFormVisible: boolean = false;
    protected websocket: Websocket = inject(Websocket);
    protected service: Service = inject(Service);
    protected initialModel: LocationModel | null = null;

    private geoResults: GeoResult[] = [];

    constructor(private translate: TranslateService) { }

    public async ngOnInit() {

        this.edge ??= await this.service.getCurrentEdge();
        const config = await this.service.getConfig();

        if (this.edge == null || config == null) {
            return;
        }

        const metaProperties = config.getComponentProperties("_meta");

        this.initialModel = {
            street: "",
            zip: metaProperties["postcode"] ?? "",
            city: metaProperties["placeName"] ?? "",
            country: "",
        };

        this.id = this.edge.getNameNumber();

        if (this.initialModel) {
            this.model = { ...this.initialModel };
        }

        this.initTranslationsAndFields();
    }

    public async validateAddress(): Promise<void> {
        if (this.form.invalid || this.edge == null) {
            return;
        }

        const { street, zip, city, country } = this.model;
        const query = `${street}, ${zip}, ${city}, ${country}`;

        this.isValidating = true;
        this.addressesAvailable = false;
        this.isBusyChange.emit(true);

        const request = new ComponentJsonApiRequest({
            componentId: "_meta",
            payload: new GeoCodeJsonRpc.Request({ query }),
        });

        const [error, response] = await PromiseUtils.Functions.handle(
            this.edge.sendRequest(this.websocket, request)
        );

        if (error) {
            console.error("GeoCode validation failed:", error);
        }

        if (response) {
            const result = (response as GeoCodeJsonRpc.Response).result.geocodingResults;
            this.geoResults = result ?? [];
            this.fillGeoCodeFields(this.geoResults);
            this.addressesAvailable = Array.isArray(this.geoResults) && this.geoResults.length > 0;
        }

        this.isValidating = false;
        this.isBusyChange.emit(false);
    }

    public async save(): Promise<SaveResult> {
        // Validation Check
        if (this.form.invalid || this.geoCodeForm.invalid || this.edge == null) {
            return { status: "INVALID" };
        }

        const selectedIndex = this.geoCodeModel.index;

        // Handle "Unknown Address" selection
        if (selectedIndex === this.unknownAddressOptionIndex) {
            return { status: "UNKNOWN_ADDRESS_SELECTED" };
        }

        // specific index validation
        if (typeof selectedIndex !== "number" || selectedIndex < 0 || selectedIndex >= this.geoResults.length) {
            console.error("Invalid GeoResult index selected.");
            return { status: "INVALID" };
        }

        const selectedGeo = this.geoResults[selectedIndex];

        // Update Backend (Edge Config)
        const properties = [
            { name: "placeName", value: selectedGeo.placeName ?? "" },
            { name: "postcode", value: selectedGeo.postcode ?? "" },
            { name: "latitude", value: selectedGeo.latitude ?? -999.0 },
            { name: "longitude", value: selectedGeo.longitude ?? -999.0 },
            { name: "timezone", value: selectedGeo.timezone ?? "" },
            { name: "subdivisionCode", value: selectedGeo.subdivisionCode?.replace("-", "_") ?? "UNDEFINED" },
        ];

        this.isSaving = true;
        this.isBusyChange.emit(true);

        const [error] = await PromiseUtils.Functions.handle(
            this.edge.updateAppConfig(this.websocket, "_meta", properties)
        );

        this.isSaving = false;
        this.isBusyChange.emit(false);

        if (error) {
            console.error("Failed to update AppConfig", error);
            return { status: "INVALID" };
        }

        this.addressesAvailable = false;
        this.geoResults = [];

        // Success case
        this.addressUpdated.emit();
        return { status: "SUCCESS", geoResult: selectedGeo };
    }

    protected async onSaveClicked() {
        await this.save();
    }

    private async initTranslationsAndFields() {
        const [error, translations] = await PromiseUtils.Functions.handle(
            Language.normalizeAdditionalTranslationFiles({ de: de, en: en })
        );

        if (error) {
            console.error("Failed to load component translations", error);
            // Fallback: generate fields anyway
            this.generateFormConfiguration();
            return;
        }

        if (translations) {
            for (const { lang, translation, shouldMerge } of translations) {
                this.translate.setTranslation(lang, translation, shouldMerge);
            }
        }

        // Translations are ready: generate fields with correct labels
        this.generateFormConfiguration();
    }

    private generateFormConfiguration() {
        this.fields = this.generateFields();
        this.geoCodeFields = [
            {
                key: "index",
                type: "radio",
                props: {
                    radioSlot: "start",
                    label: this.translate.instant("SYSTEM_LOCATION_VALIDATOR.GEO_CODE_RESULT_LABEL"),
                    options: [],
                },
            },
        ];
    }

    private fillGeoCodeFields(results: GeoResult[]) {
        const options: ComponentData[] = [];
        let defaultIndex = 0;

        results.forEach((geoCode, index) => {
            const hasNotMinRequirements = Object.values(ObjectUtils.pickProperties(geoCode, ["country", "postcode", "placeName"])).some((el) => el === null);

            const street = geoCode.road && geoCode.houseNumber
                ? `${geoCode.road} ${geoCode.houseNumber}, `
                : "";

            const label = `${street}${geoCode.postcode ?? ""}, ${geoCode.placeName ?? ""}, ${geoCode.country ?? ""}`;

            if (hasNotMinRequirements) {
                return;
            }

            options.push({ value: index, label });

            if (this.isMatchingAddress(geoCode)) {
                defaultIndex = index;
            }
        });

        // Add "Unknown" option
        if (this.context === "INSTALLATION" && (!this.hasMatchingAddress(results) || options.length === 0)) {
            this.unknownAddressOptionIndex = options.length;

            options.push({
                value: options.length,
                label: this.translate.instant("SYSTEM_LOCATION_VALIDATOR.UNKNOWN_ADDRESS"),
                default: options.length === 0,
            });
        } else {
            this.unknownAddressOptionIndex = null;
        }

        // Update Formly Options safely
        this.geoCodeFields = FormlyUtils.changeFormlyFieldProps("index", this.geoCodeFields, (props) => {
            if (props) {
                props.options = options;
            }
            return props;
        });

        this.isGeoFormVisible = options.length > 0;

        if (this.isGeoFormVisible) {
            this.geoCodeModel.index = defaultIndex;
        }
    }

    private generateFields(): FormlyFieldConfig[] {
        return [
            {
                key: "street",
                type: "input",
                templateOptions: {
                    label: this.translate.instant("INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.STREET_ADDRESS"),
                    required: true,
                },
            },
            {
                key: "zip",
                type: "input",
                templateOptions: {
                    label: this.translate.instant("INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.ZIP"),
                    required: true,
                },
                validators: { validation: ["zip"] },
            },
            {
                key: "city",
                type: "input",
                templateOptions: {
                    label: this.translate.instant("INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.CITY"),
                    required: true,
                },
            },
            {
                key: "country",
                type: "select",
                templateOptions: {
                    label: this.translate.instant("INSTALLATION.PROTOCOL_INSTALLER_AND_CUSTOMER.COUNTRY"),
                    required: true,
                    options: COUNTRY_OPTIONS(this.translate),
                },
            },
        ];
    }

    private hasMatchingAddress(results?: GeoResult[]): boolean {
        return results ? results.some(r => this.isMatchingAddress(r)) : false;
    }

    private isMatchingAddress(geo: GeoResult): boolean {
        const street = geo.road && geo.houseNumber ? `${geo.road} ${geo.houseNumber}` : "";
        return street === (this.model.street).replaceAll(",", "") &&
            geo.postcode === this.model.zip &&
            geo.placeName === this.model.city &&
            CountryUtils.fromCountryCode(geo.country ?? "") === this.model.country;
    }
}
