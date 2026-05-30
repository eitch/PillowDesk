package ch.eitchnet.pillowdesk.rest;

import ch.eitchnet.pillowdesk.core.search.RateOverrideSearch;
import ch.eitchnet.pillowdesk.core.service.AddRateOverrideService;
import ch.eitchnet.pillowdesk.core.service.RemoveRateOverrideService;
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
import li.strolch.service.StringArgument;
import li.strolch.service.api.ServiceHandler;
import li.strolch.service.api.ServiceResult;

import java.util.List;

import static li.strolch.rest.StrolchRestfulConstants.STROLCH_CERTIFICATE;
import static li.strolch.utils.helper.ExceptionHelper.getCallerMethod;

@Path("pillowdesk/rate-overrides")
@Tag(name = "Rate Overrides", description = "Endpoints for managing rate overrides.")
public class RateOverrideResource {

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
	public Response getRateOverrides(@Context HttpServletRequest request, @QueryParam("rateId") String rateId) {
		Certificate cert = getCertificate(request);
		try (StrolchTransaction tx = openTx(cert)) {
			RateOverrideSearch search = new RateOverrideSearch();
			if (rateId != null && !rateId.isEmpty())
				search.forRate(rateId);

			List<Resource> overrides = search.search(tx).toList();
			return ResponseUtil.listToResponse("data", overrides, r -> r.accept(new StrolchRootElementToJsonVisitor()));
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response addRateOverride(@Context HttpServletRequest request, String data) {
		Certificate cert = getCertificate(request);
		JsonObject jsonObject = JsonParser.parseString(data).getAsJsonObject();
		Resource rateOverride = new ResourceFromJsonVisitor().visit(jsonObject);

		ServiceResult result = getServiceHandler().doService(cert, new AddRateOverrideService(),
				new AddRateOverrideService.AddRateOverrideArg(rateOverride));
		return ResponseUtil.toResponse(result);
	}

	@DELETE
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response removeRateOverride(@Context HttpServletRequest request, @PathParam("id") String id) {
		Certificate cert = getCertificate(request);

		ServiceResult result = getServiceHandler().doService(cert, new RemoveRateOverrideService(),
				new StringArgument(id));
		return ResponseUtil.toResponse(result);
	}
}
