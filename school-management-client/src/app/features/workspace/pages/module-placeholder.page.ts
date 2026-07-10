import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { FooterComponent } from '../../../core/layout/footer/footer.component';
import { HeaderComponent } from '../../../core/layout/header/header.component';

@Component({
  selector: 'app-module-placeholder-page',
  imports: [HeaderComponent, FooterComponent, RouterLink, TranslatePipe],
  templateUrl: './module-placeholder.page.html',
  styleUrl: './module-placeholder.page.css'
})
export class ModulePlaceholderPage {
  private readonly route = inject(ActivatedRoute);

  protected readonly moduleKey = computed(() => this.route.snapshot.data['moduleKey'] as string);
  protected readonly titleKey = computed(() => `workspace.modules.${this.moduleKey()}.title`);
  protected readonly descriptionKey = computed(() => `workspace.modules.${this.moduleKey()}.description`);
}

