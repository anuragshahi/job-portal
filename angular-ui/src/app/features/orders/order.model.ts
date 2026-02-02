export interface Order {
  id: number;
  orderNumber: string;
  status: OrderStatus;
  createdBy: string;
  creationTime: string;
}

export type OrderStatus = 'CREATED' | 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELED';

export interface CreateOrderRequest {
  // The backend auto-generates orderNumber and sets createdBy from JWT
}
