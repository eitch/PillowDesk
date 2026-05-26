package ch.eitchnet.pillowdesk.core.service;

import ch.eitchnet.pillowdesk.core.policy.StayCalculatorPolicy;
import li.strolch.model.Order;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.service.api.AbstractService;
import li.strolch.service.api.ServiceArgument;
import li.strolch.service.api.ServiceResult;

import static ch.eitchnet.pillowdesk.core.policy.StayValidationPolicy.*;

public class AddStayService extends AbstractService<AddStayService.AddStayArg, ServiceResult> {

	public static class AddStayArg extends ServiceArgument {
		public Order stay;
	}

	@Override
	protected ServiceResult internalDoService(AddStayArg arg) {
		if (arg.stay == null)
			return ServiceResult.error("Stay is missing!");

		try (StrolchTransaction tx = openArgOrUserTx(arg)) {
			validate(tx, arg.stay);
			generateBookingId(tx, arg.stay);

			StayCalculatorPolicy.calculateAndFill(tx, arg.stay);
			tx.add(arg.stay);
			tx.commitOnClose();
		} catch (Exception e) {
			return ServiceResult.error(e.getMessage());
		}

		return ServiceResult.success();
	}

	@Override
	protected ServiceResult getResultInstance() {
		return new ServiceResult();
	}

	@Override
	public AddStayArg getArgumentInstance() {
		return new AddStayArg();
	}
}
