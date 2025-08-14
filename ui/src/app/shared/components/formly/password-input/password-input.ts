import { Component, Input } from "@angular/core";
import { FormControl } from "@angular/forms";

@Component({
  selector: "oe-password-input",
  templateUrl: "./password-input.html",
  standalone: false,
})
export class PasswordInputComponent {
  @Input() public control!: FormControl;
  @Input() public placeholder: string = "";
  @Input() public label: string = "";
  @Input() public required: boolean = false;
  @Input() public ariaLabelShow: string = "Show password";
  @Input() public ariaLabelHide: string = "Hide password";

  protected showPassword: boolean = false;

  protected togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }
}
