package ch.eitchnet.pillowdesk.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import li.strolch.model.Resource;
import li.strolch.model.json.StrolchRootElementToJsonVisitor;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.privilege.model.Certificate;
import li.strolch.rest.RestfulStrolchComponent;
import li.strolch.rest.helper.ResponseUtil;

import java.util.List;

import static ch.eitchnet.pillowdesk.core.model.ModelConstants.TYPE_RATE;
import static li.strolch.rest.StrolchRestfulConstants.STROLCH_CERTIFICATE;

@Path("pillowdesk/rates")
public class RateResource {

	private Certificate getCertificate(HttpServletRequest request) {
		return (Certificate) request.getAttribute(STROLCH_CERTIFICATE);
	}

	private StrolchTransaction openTx(Certificate cert) {
		return RestfulStrolchComponent.getInstance().openTx(cert, RateResource.class.getSimpleName());
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getRates(@Context HttpServletRequest request) {

		Certificate cert = getCertificate(request);
		try (StrolchTransaction tx = openTx(cert)) {
			List<Resource> rates = tx.getResourceMap().getElementsBy(tx, TYPE_RATE);
			return ResponseUtil.toResponse(rates, r -> r.accept(new StrolchRootElementToJsonVisitor()));
		}
	}
}
