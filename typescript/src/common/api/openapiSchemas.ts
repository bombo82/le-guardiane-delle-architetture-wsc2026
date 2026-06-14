// Schemas OpenAPI condivisi per i DTO request/response.
// In Java i DTO non hanno annotazioni OpenAPI proprie; le definizioni dei tipi
// sono referenziate nelle annotazioni dei controller. Per parità, gli schemas
// sono centralizzati qui vicino alla configurazione OpenAPI.

/**
 * @swagger
 * components:
 *   schemas:
 *     PlaceBookingRequest:
 *       type: object
 *       required:
 *         - amount
 *         - description
 *         - giftCardId
 *       properties:
 *         amount:
 *           type: number
 *           description: Importo della prenotazione in euro.
 *         description:
 *           type: string
 *           description: Descrizione della prenotazione.
 *         giftCardId:
 *           type: string
 *           format: uuid
 *           description: Identificativo della gift card da utilizzare.
 */

/**
 * @swagger
 * components:
 *   schemas:
 *     BookingResponse:
 *       type: object
 *       properties:
 *         id:
 *           type: string
 *           format: uuid
 *         description:
 *           type: string
 *         giftCardId:
 *           type: string
 *           format: uuid
 */

/**
 * @swagger
 * components:
 *   schemas:
 *     RequestTopUpRequest:
 *       type: object
 *       required:
 *         - amount
 *       properties:
 *         amount:
 *           type: number
 *           description: Importo della ricarica in euro.
 */

/**
 * @swagger
 * components:
 *   schemas:
 *     GiftCardResponse:
 *       type: object
 *       properties:
 *         id:
 *           type: string
 *           format: uuid
 *         balance:
 *           type: number
 *           description: Saldo residuo della gift card in euro.
 */

/**
 * @swagger
 * components:
 *   schemas:
 *     CreatePaymentRequest:
 *       type: object
 *       required:
 *         - paymentId
 *         - clientReference
 *         - amount
 *         - requestedAt
 *       properties:
 *         paymentId:
 *           type: string
 *           format: uuid
 *         clientReference:
 *           type: string
 *         amount:
 *           type: number
 *         requestedAt:
 *           type: string
 *           format: date-time
 */

/**
 * @swagger
 * components:
 *   schemas:
 *     StartTransactionRequest:
 *       type: object
 *       required:
 *         - provider
 *         - amount
 *       properties:
 *         provider:
 *           type: string
 *           enum: [paypal, klarna, giftcard]
 *           description: Provider con cui avviare la transazione.
 *         providerReference:
 *           type: string
 *           format: uuid
 *           nullable: true
 *           description: Riferimento esterno della transazione, se disponibile.
 *         amount:
 *           type: number
 */

/**
 * @swagger
 * components:
 *   schemas:
 *     TransactionResponse:
 *       type: object
 *       properties:
 *         id:
 *           type: string
 *           format: uuid
 *         provider:
 *           type: string
 *         providerReference:
 *           type: string
 *           format: uuid
 *           nullable: true
 *         amount:
 *           type: number
 *         status:
 *           type: string
 *           enum: [STARTED, ACCEPTED, REJECTED, EXPIRED, REFUNDED]
 *         startedAt:
 *           type: string
 *           format: date-time
 *         completedAt:
 *           type: string
 *           format: date-time
 *           nullable: true
 */

/**
 * @swagger
 * components:
 *   schemas:
 *     PaymentDetailsResponse:
 *       type: object
 *       properties:
 *         id:
 *           type: string
 *           format: uuid
 *         clientReference:
 *           type: string
 *         amount:
 *           type: number
 *         status:
 *           type: string
 *           enum: [PENDING, ACCEPTED, REJECTED, EXPIRED]
 *         requestedAt:
 *           type: string
 *           format: date-time
 *         transactions:
 *           type: array
 *           items:
 *             $ref: '#/components/schemas/TransactionResponse'
 */
