// Eccezione di dominio lanciata quando una Transaction non viene trovata.

export class TransactionNotFoundException extends Error {
  constructor(message: string = 'transaction not found') {
    super(message);
    this.name = 'TransactionNotFoundException';
  }
}
