// API pubbliche del Booking Bounded Context.

import express, { type Express, type ErrorRequestHandler, type Request, type Response } from 'express';
import { getErrorMessage } from '@/common/api/errorMessage.js';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Description } from '@/common/domain/primitive/description.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import { BookingQueryService } from '../application/query/bookingQueryService.js';
import { BookingPlacing } from '../application/usecases/bookingPlacing.js';
import { BookingId } from '../domain/booking/bookingId.js';
import { placeBooking } from '../application/commands/placeBooking.js';
import { parsePlaceBookingRequest } from './placeBookingRequest.js';
import { toBookingResponse } from './bookingResponse.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class BookingApi {
  private readonly _bookingPlacing: BookingPlacing;
  private readonly _bookingQueryService: BookingQueryService;

  constructor(bookingPlacing: BookingPlacing, bookingQueryService: BookingQueryService) {
    requireDependency(bookingPlacing, "bookingPlacing");
    requireDependency(bookingQueryService, "bookingQueryService");
    this._bookingPlacing = bookingPlacing;
    this._bookingQueryService = bookingQueryService;
  }

  configure(app: Express): void {
    const router = express.Router();
    router.use(express.json());

    router.post('/bookings', (req, res) => this.place(req, res));
    router.get('/bookings/:id', (req, res) => this.getById(req, res));

    const errorHandler: ErrorRequestHandler = (err, _req, res, next) => {
      if (err instanceof SyntaxError && 'body' in err) {
        res.status(400).send('request body is required');
        return;
      }
      next(err);
    };
    router.use(errorHandler);

    app.use(router);
  }

  /**
   * @swagger
   * /bookings:
   *   post:
   *     summary: Crea una nuova prenotazione
   *     tags: [Booking]
   *     requestBody:
   *       required: true
   *       content:
   *         application/json:
   *           schema:
   *             $ref: '#/components/schemas/PlaceBookingRequest'
   *     responses:
   *       201:
   *         description: Prenotazione creata
   *         headers:
   *           Location:
   *             description: URI della prenotazione creata
   *             schema:
   *               type: string
   *         content:
   *           application/json:
   *             schema:
   *               $ref: '#/components/schemas/BookingResponse'
   *       400:
   *         description: Richiesta non valida
   */
  private place(req: Request, res: Response): void {
    let request;
    try {
      request = parsePlaceBookingRequest(req.body);
    } catch (error) {
      res.status(400).send(getErrorMessage(error, 'bad request'));
      return;
    }

    let amount: Money;
    let description: Description;
    let giftCardId: GiftCardId;
    try {
      amount = new Money(Number(request.amount));
      description = new Description(request.description);
      giftCardId = new GiftCardId(Uuid.fromString(request.giftCardId));
    } catch (error) {
      res.status(400).send(getErrorMessage(error, 'bad request'));
      return;
    }

    const command = placeBooking(
      generateId((value) => new BookingId(value)),
      amount,
      description,
      giftCardId
    );

    let event;
    try {
      event = this._bookingPlacing.invoke(command);
    } catch (error) {
      res.status(400).send(getErrorMessage(error, 'bad request'));
      return;
    }

    const booking = this._bookingQueryService.findById(event.aggregateId);
    if (booking === null) {
      res.status(500).send('booking not found after placing');
      return;
    }

    res.status(201)
      .set('Location', `/bookings/${event.aggregateId.value.value}`)
      .json(toBookingResponse(booking));
  }

  /**
   * @swagger
   * /bookings/{id}:
   *   get:
   *     summary: Recupera una prenotazione per ID
   *     tags: [Booking]
   *     parameters:
   *       - in: path
   *         name: id
   *         required: true
   *         schema:
   *           type: string
   *           format: uuid
   *     responses:
   *       200:
   *         description: Prenotazione trovata
   *         content:
   *           application/json:
   *             schema:
   *               $ref: '#/components/schemas/BookingResponse'
   *       400:
   *         description: ID non valido
   *       404:
   *         description: Prenotazione non trovata
   */
  private getById(req: Request, res: Response): void {
    const id = this.parseBookingId(req, res);
    if (id === null) {
      return;
    }

    const booking = this._bookingQueryService.findById(id);
    if (booking === null) {
      res.status(404).send();
      return;
    }

    res.status(200).json(toBookingResponse(booking));
  }

  private parseBookingId(req: Request, res: Response): BookingId | null {
    const idParam = req.params.id;
    if (typeof idParam !== 'string') {
      res.status(400).send('Invalid booking id format');
      return null;
    }
    try {
      return new BookingId(Uuid.fromString(idParam));
    } catch (_error) {
      res.status(400).send('Invalid booking id format');
      return null;
    }
  }
}
