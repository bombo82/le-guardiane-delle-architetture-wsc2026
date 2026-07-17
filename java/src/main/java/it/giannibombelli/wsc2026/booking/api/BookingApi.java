package it.giannibombelli.wsc2026.booking.api;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.openapi.*;
import it.giannibombelli.wsc2026.booking.application.commands.PlaceBooking;
import it.giannibombelli.wsc2026.booking.application.query.BookingDetails;
import it.giannibombelli.wsc2026.booking.application.query.BookingQueryService;
import it.giannibombelli.wsc2026.booking.application.usecases.BookingPlacing;
import it.giannibombelli.wsc2026.booking.domain.booking.BookingId;
import it.giannibombelli.wsc2026.booking.domain.events.BookingPlaced;
import it.giannibombelli.wsc2026.booking.domain.primitive.GiftCardReference;
import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.module.WebApi;

import java.util.UUID;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

public final class BookingApi implements WebApi {
    private final BookingPlacing bookingPlacing;
    private final BookingQueryService bookingQueryService;

    public BookingApi(BookingPlacing bookingPlacing, BookingQueryService bookingQueryService) {
        this.bookingPlacing = bookingPlacing;
        this.bookingQueryService = bookingQueryService;
    }

    @Override
    public void configure(JavalinConfig config) {
        config.routes.apiBuilder(() -> {
            post("/bookings", this::place);
            get("/bookings/{id}", this::getById);
        });
    }

    @OpenApi(
        path = "/bookings",
        methods = HttpMethod.POST,
        summary = "Place a new booking",
        tags = {"Booking"},
        requestBody = @OpenApiRequestBody(content = {@OpenApiContent(from = PlaceBookingRequest.class)}),
        responses = {
            @OpenApiResponse(status = "201", content = {@OpenApiContent(from = BookingResponse.class)}),
            @OpenApiResponse(status = "400")
        }
    )
    private void place(Context ctx) {
        PlaceBookingRequest request = parsePlaceBookingRequest(ctx);
        if (request == null) {
            return;
        }

        if (request.amount() == null) {
            ctx.status(400).result("amount is required");
            return;
        }
        if (request.description() == null || request.description().isBlank()) {
            ctx.status(400).result("description is required");
            return;
        }
        if (request.giftCardId() == null) {
            ctx.status(400).result("giftCardId is required");
            return;
        }

        Money amount;
        try {
            amount = new Money(request.amount());
        } catch (IllegalArgumentException ex) {
            ctx.status(400).result(ex.getMessage());
            return;
        }

        Description description;
        try {
            description = new Description(request.description());
        } catch (IllegalArgumentException ex) {
            ctx.status(400).result(ex.getMessage());
            return;
        }

        GiftCardReference giftCardReference;
        try {
            giftCardReference = new GiftCardReference(request.giftCardId());
        } catch (IllegalArgumentException ex) {
            ctx.status(400).result(ex.getMessage());
            return;
        }

        BookingId bookingId = EntityId.generate(BookingId::new);
        PlaceBooking command = new PlaceBooking(bookingId, amount, description, giftCardReference);

        BookingPlaced event;
        try {
            event = bookingPlacing.invoke(command);
        } catch (IllegalArgumentException ex) {
            ctx.status(400).result(ex.getMessage());
            return;
        }

        var bookingOpt = bookingQueryService.findById(event.aggregateId());
        if (bookingOpt.isEmpty()) {
            ctx.status(500).result("booking not found after creation");
            return;
        }

        ctx.status(201)
            .header("Location", "/bookings/" + event.aggregateId().value())
            .json(toResponse(bookingOpt.get()));
    }

    @OpenApi(
        path = "/bookings/{id}",
        methods = HttpMethod.GET,
        summary = "Get a booking by id",
        tags = {"Booking"},
        pathParams = {@OpenApiParam(name = "id", required = true)},
        responses = {
            @OpenApiResponse(status = "200", content = {@OpenApiContent(from = BookingResponse.class)}),
            @OpenApiResponse(status = "400"),
            @OpenApiResponse(status = "404")
        }
    )
    private void getById(Context ctx) {
        BookingId id = parseBookingId(ctx);
        if (id == null) {
            return;
        }

        var bookingOpt = bookingQueryService.findById(id);
        if (bookingOpt.isEmpty()) {
            ctx.status(404);
            return;
        }

        ctx.status(200).json(toResponse(bookingOpt.get()));
    }

    private PlaceBookingRequest parsePlaceBookingRequest(Context ctx) {
        try {
            return ctx.bodyAsClass(PlaceBookingRequest.class);
        } catch (Exception ex) {
            ctx.status(400).result("request body is required");
            return null;
        }
    }

    private BookingId parseBookingId(Context ctx) {
        String idParam = ctx.pathParam("id");
        try {
            return new BookingId(UUID.fromString(idParam));
        } catch (IllegalArgumentException ex) {
            ctx.status(400).result("Invalid booking id format");
            return null;
        }
    }

    static BookingResponse toResponse(BookingDetails booking) {
        return new BookingResponse(booking.id(), booking.description().value(), booking.giftCardId());
    }
}
