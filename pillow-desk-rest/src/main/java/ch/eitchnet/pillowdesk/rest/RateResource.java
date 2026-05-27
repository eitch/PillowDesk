package ch.eitchnet.pillowdesk.rest;

import ch.eitchnet.pillowdesk.core.search.RateSearch;
import ch.eitchnet.pillowdesk.core.service.AddRateService;
import ch.eitchnet.pillowdesk.core.service.RemoveRateService;
import ch.eitchnet.pillowdesk.core.service.UpdateRateService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import li.strolch.model.Resource;
import li.strolch.model.json.ResourceFromJsonVisitor;
import li.strolch.model.json.StrolchRootElementToJsonVisitor;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.privilege.model.Certificate;
import li.strolch.rest.RestfulStrolchComponent;
import li.strolch.rest.helper.ResponseUtil;
import li.strolch.service.api.ServiceHandler;
import li.strolch.service.api.ServiceResult;

import java.util.List;

import static ch.eitchnet.pillowdesk.core.model.ModelConstants.TYPE_RATE;
import static li.strolch.rest.StrolchRestfulConstants.STROLCH_CERTIFICATE;
import static li.strolch.utils.helper.ExceptionHelper.getCallerMethod;

@Path("pillowdesk/rates")
@Tag(name = "Rates", description = "Endpoints for managing rates.")
public class RateResource {

	private static Certificate getCertificate(HttpServletRequest request) {
		return (Certificate) request.getAttribute(STROLCH_CERTIFICATE);
	}

	private static StrolchTransaction openTx(Certificate certificate) {
		return RestfulStrolchComponent.getInstance().openTx(certificate, getCallerMethod(2));
	}

	private static ServiceHandler getServiceHandler() {
		return RestfulStrolchComponent.getInstance().getComponent(ServiceHandler.class);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getRates(@Context HttpServletRequest request, @QueryParam("name") String name) {

		Certificate cert = getCertificate(request);
		try (StrolchTransaction tx = openTx(cert)) {
			RateSearch search = new RateSearch();
			search.name(name);

			List<Resource> rates = search.search(tx).orderByName(false).toList();
			return ResponseUtil.listToResponse("data", rates, r -> r.accept(new StrolchRootElementToJsonVisitor()));
		}
	}

	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getRate(@Context HttpServletRequest request, @PathParam("id") String id) {

		Certificate cert = getCertificate(request);
		try (StrolchTransaction tx = openTx(cert)) {
			Resource rate = tx.getResourceBy(TYPE_RATE, id);
			if (rate == null) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}

			return ResponseUtil.toResponse("data", rate.accept(new StrolchRootElementToJsonVisitor()));
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response addRate(@Context HttpServletRequest request, String data) {

		Certificate cert = getCertificate(request);
		JsonObject jsonObject = JsonParser.parseString(data).getAsJsonObject();
		Resource rate = new ResourceFromJsonVisitor().visit(jsonObject);

		AddRateService service = new AddRateService();
		AddRateService.AddRateArg arg = new AddRateService.AddRateArg();
		arg.rate = rate;

		ServiceResult result = getServiceHandler().doService(cert, service, arg);
		return ResponseUtil.toResponse(result);
	}

	@PUT
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateRate(@Context HttpServletRequest request, @PathParam("id") String id, String data) {

		Certificate cert = getCertificate(request);
		JsonObject jsonObject = JsonParser.parseString(data).getAsJsonObject();
		Resource rate = new ResourceFromJsonVisitor().visit(jsonObject);
		rate.setId(id); // Ensure ID is set from path

		UpdateRateService service = new UpdateRateService();
		UpdateRateService.UpdateRateArg arg = new UpdateRateService.UpdateRateArg();
		arg.rate = rate;

		ServiceResult result = getServiceHandler().doService(cert, service, arg);
		return ResponseUtil.toResponse(result);
	}

	@DELETE
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response removeRate(@Context HttpServletRequest request, @PathParam("id") String id) {

		Certificate cert = getCertificate(request);

		RemoveRateService service = new RemoveRateService();
		RemoveRateService.RemoveRateArg arg = new RemoveRateService.RemoveRateArg();
		arg.id = id;

		ServiceResult result = getServiceHandler().doService(cert, service, arg);
		return ResponseUtil.toResponse(result);
	}
}
