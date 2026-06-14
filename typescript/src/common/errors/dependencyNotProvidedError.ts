export class DependencyNotProvidedError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'DependencyNotProvidedError';
  }
}
