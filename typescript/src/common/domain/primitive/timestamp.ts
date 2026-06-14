import { requireArgument } from '@/common/utils/requireArgument.js';
export class Timestamp {
  readonly value: Date;

  constructor(value: Date) {
    requireArgument(value, 'timestamp');
    this.value = value;
  }

  plusSeconds(seconds: number): Timestamp {
    return new Timestamp(new Date(this.value.getTime() + seconds * 1000));
  }

  minusSeconds(seconds: number): Timestamp {
    return new Timestamp(new Date(this.value.getTime() - seconds * 1000));
  }

  isBefore(other: Timestamp): boolean {
    return this.value.getTime() < other.value.getTime();
  }

  isAfter(other: Timestamp): boolean {
    return this.value.getTime() > other.value.getTime();
  }

  isBeforeOrEqual(other: Timestamp): boolean {
    return this.value.getTime() <= other.value.getTime();
  }

  isAfterOrEqual(other: Timestamp): boolean {
    return this.value.getTime() >= other.value.getTime();
  }

  static now(): Timestamp {
    return new Timestamp(new Date());
  }
}
