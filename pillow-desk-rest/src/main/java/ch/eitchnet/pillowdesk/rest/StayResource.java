package ch.eitchnet.pillowdesk.rest;

import ch.eitchnet.pillowdesk.core.policy.StayCalculatorPolicy;
import ch.eitchnet.pillowdesk.core.search.StaySearch;
import ch.eitchnet.pillowdesk.core.service.AddStayService;
import ch.eitchnet.pillowdesk.core.service.RemoveStayService;
import ch.eitchnet.pillowdesk.core.service.UpdateStayService;
import ch.eitchnet.pillowdesk.core.service.UpdateStayService.UpdateStayArg;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import li.strolch.model.Order;
import li.strolch.model.Resource;
import li.strolch.model.json.OrderFromJsonVisitor;
import li.strolch.model.json.StrolchRootElementToJsonVisitor;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.privilege.model.Certificate;
import li.strolch.rest.RestfulStrolchComponent;
import li.strolch.rest.helper.ResponseUtil;
import li.strolch.service.api.ServiceHandler;
import li.strolch.service.api.ServiceResult;
import li.strolch.utils.helper.StringHelper;

import java.time.ZonedDateTime;
import java.util.List;

import static ch.eitchnet.pillowdesk.core.model.ModelConstants.*;
import static ch.eitchnet.pillowdesk.core.service.AddStayService.AddStayArg;
import static ch.eitchnet.pillowdesk.core.service.RemoveStayService.RemoveStayArg;
import static li.strolch.rest.StrolchRestfulConstants.STROLCH_CERTIFICATE;
import static li.strolch.utils.helper.ExceptionHelper.getCallerMethod;
import static li.strolch.utils.iso8601.ISO8601.parseToZdt;

@Path("pillowdesk/stays")
@Tag(name = "Stays", description = "Endpoints for managing stays.")
public class StayResource {

	private static Certificate getCertificate(HttpServletRequest request) {
		return (Certificate) request.getAttribute(STROLCH_CERTIFICATE);
	}

	private static StrolchTransaction openTx(Certificate certificate) {
		return RestfulStrolchComponent.getInstance().openTx(certificate, getCallerMethod());
	}

	private static ServiceHandler getServiceHandler() {
		return RestfulStrolchComponent.getInstance().getComponent(ServiceHandler.class);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response searchStays(@Context HttpServletRequest request, @QueryParam("name") String name,
			@QueryParam("from") String fromStr, @QueryParam("to") String toStr,
			@QueryParam("isAirBnb") Boolean isAirBnb, @QueryParam("bookingId") String bookingId) {

		Certificate cert = getCertificate(request);
		try (StrolchTransaction tx = openTx(cert)) {
			StaySearch search = new StaySearch();
			search.guestName(name);
			search.isAirBnb(isAirBnb);
			search.bookingId(bookingId);

			if (StringHelper.isNotEmpty(fromStr) && StringHelper.isNotEmpty(toStr)) {
				ZonedDateTime from = parseToZdt(fromStr);
				ZonedDateTime to = parseToZdt(toStr);
				search.dateRange(from, to);
			}

			List<Order> stays = search.search(tx).orderByParam(PARAM_CHECK_IN, true).cloneIfReadOnly().toList();
			return ResponseUtil.listToResponse("data", stays, order -> {
				if (!order.hasParameter(BAG_PARAMETERS, PARAM_TOTAL_REVENUE)) {
					StayCalculatorPolicy.calculateAndFill(tx, order);
				}
				return order.accept(new StrolchRootElementToJsonVisitor());
			});
		}
	}

	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getStay(@Context HttpServletRequest request, @PathParam("id") String id) {

		Certificate cert = getCertificate(request);
		try (StrolchTransaction tx = openTx(cert)) {
			Order stay = tx.getOrderBy(TYPE_STAY, id);
			if (stay == null) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}

			if (!stay.hasParameter(BAG_PARAMETERS, PARAM_TOTAL_REVENUE)) {
				StayCalculatorPolicy.calculateAndFill(tx, stay);
			}

			return ResponseUtil.toResponse("data", stay.accept(new StrolchRootElementToJsonVisitor()));
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response addStay(@Context HttpServletRequest request, String data) {

		Certificate cert = getCertificate(request);
		JsonObject jsonObject = JsonParser.parseString(data).getAsJsonObject();
		Order stay = new OrderFromJsonVisitor().visit(jsonObject);

		AddStayService service = new AddStayService();
		AddStayArg arg = new AddStayArg();
		arg.stay = stay;

		ServiceResult result = getServiceHandler().doService(cert, service, arg);
		return ResponseUtil.toResponse(result);
	}

	@POST
	@Path("calculate")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response calculateCosts(@Context HttpServletRequest request, String data) {
		Certificate cert = getCertificate(request);
		JsonObject jsonObject = JsonParser.parseString(data).getAsJsonObject();
		Order stay = new OrderFromJsonVisitor().visit(jsonObject);

		try (StrolchTransaction tx = openTx(cert)) {
			if (!stay.hasParameterBag(BAG_RELATIONS) || !stay.getParameterBag(BAG_RELATIONS).hasParameter(PARAM_RATE))
				return Response.status(Response.Status.BAD_REQUEST).entity("Missing rate relation").build();

			Resource rate = tx.getResourceByRelation(stay, PARAM_RATE, true);
			StayCalculatorPolicy.StayCosts costs = StayCalculatorPolicy.calculate(stay, rate);

			JsonObject result = new JsonObject();
			result.addProperty("nights", costs.nights());
			result.addProperty("touristTax", costs.touristTax());
			result.addProperty("totalRevenue", costs.totalGross());

			return ResponseUtil.toResponse("data", result);
		}
	}

	@PUT
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateStay(@Context HttpServletRequest request, @PathParam("id") String id, String data) {

		Certificate cert = getCertificate(request);
		JsonObject jsonObject = JsonParser.parseString(data).getAsJsonObject();
		Order stay = new OrderFromJsonVisitor().visit(jsonObject);
		stay.setId(id); // Ensure ID is set from path

		UpdateStayService service = new UpdateStayService();
		UpdateStayArg arg = new UpdateStayArg();
		arg.stay = stay;

		ServiceResult result = getServiceHandler().doService(cert, service, arg);
		return ResponseUtil.toResponse(result);
	}

	@DELETE
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response removeStay(@Context HttpServletRequest request, @PathParam("id") String id) {

		Certificate cert = getCertificate(request);

		RemoveStayService service = new RemoveStayService();
		RemoveStayArg arg = new RemoveStayArg();
		arg.type = TYPE_STAY;
		arg.id = id;

		ServiceResult result = getServiceHandler().doService(cert, service, arg);
		return ResponseUtil.toResponse(result);
	}
}
