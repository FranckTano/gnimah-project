import { Component, Input, Output, EventEmitter } from '@angular/core';
import { FormGroup } from '@angular/forms';

@Component({
  selector: 'app-client-form',
  standalone: false,
  templateUrl: './client-form.component.html',
  styleUrl: './client-form.component.scss'
})
export class ClientFormComponent {
  @Input() form!: FormGroup;
  @Input() civiliteOptions: { label: string; value: string }[] = [];
  @Input() typePieceOptions: { label: string; value: string }[] = [];
  @Output() formSubmit = new EventEmitter<void>();

  isFieldInvalid(field: string): boolean {
    const ctrl = this.form?.get(field);
    return !!(ctrl && ctrl.invalid && ctrl.touched);
  }

  onSubmit(): void {
    if (this.form.valid) {
      this.formSubmit.emit();
    } else {
      this.form.markAllAsTouched();
    }
  }
}
