import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Order } from './order.model';

@Injectable({
  providedIn: 'root'
})
export class OrdersService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.bffUrl}/api/orders`;

  async getOrders(): Promise<Order[]> {
    return firstValueFrom(this.http.get<Order[]>(this.apiUrl));
  }

  async createOrder(): Promise<Order> {
    return firstValueFrom(this.http.post<Order>(this.apiUrl, {}));
  }
}
