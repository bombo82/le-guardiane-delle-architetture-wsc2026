export class PaymentNotFoundException extends Error {
  constructor(message: string = 'payment not found') {
    super(message);
    this.name = 'PaymentNotFoundException';
  }
}
