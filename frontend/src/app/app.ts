import { Component } from '@angular/core';
import { MapComponent } from './map/map.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [MapComponent],
  template: '<app-map></app-map>',
  styles: []
})
export class App {
  title = 'Texas Explorer';
}