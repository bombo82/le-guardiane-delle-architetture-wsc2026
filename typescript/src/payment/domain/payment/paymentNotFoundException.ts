// Eccezione di dominio lanciata quando un Payment non viene trovato.

export class PaymentNotFoundException extends Error {
  constructor(message: string = 'payment not found') {
    super(message);
    this.name = 'PaymentNotFoundException';
  }
}
