import { NgModule } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { FormlyModule } from "@ngx-formly/core";

import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { ModalComponentsModule } from "src/app/shared/components/modal/modal.module";
import { DirectiveModule } from "src/app/shared/directive/directive";
import { PipeModule } from "src/app/shared/pipe/pipe.module";
import { HelpButtonComponent } from "../../../../shared/components/modal/help-button/help-button";
import { AdminStorageModalComponent } from "./admin-modal/admin-modal.component";
import { StorageSystemComponent } from "./admin-modal/storage-system/storage-system";
import { InstallerOwnerGuestStorageModalComponent } from "./installer-owner-guest-modal/installer-owner-guest-modal.component";
import { StorageComponent } from "./storage.component";

@NgModule({
    imports: [
        ComponentsBaseModule,
        ModalComponentsModule,
        CommonUiModule,

        FormsModule,
        FormlyModule,
        ReactiveFormsModule,
        HelpButtonComponent,
        DirectiveModule,
        PipeModule,
        StorageSystemComponent,
        AdminStorageModalComponent,
        InstallerOwnerGuestStorageModalComponent,
    ],
    declarations: [
        StorageComponent,
    ],
    exports: [
        StorageComponent,
    ],
})
export class StorageLiveModule { }
