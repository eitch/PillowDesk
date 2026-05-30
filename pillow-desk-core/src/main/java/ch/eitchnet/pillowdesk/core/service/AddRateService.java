package ch.eitchnet.pillowdesk.core.service;

import li.strolch.model.Resource;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.service.api.AbstractService;
import li.strolch.service.api.ServiceArgument;
import li.strolch.service.api.ServiceResult;

public class AddRateService extends AbstractService<AddRateService.AddRateArg, ServiceResult> {

	public static class AddRateArg extends ServiceArgument {
		public Resource rate;
		public AddRateArg() {}
		public AddRateArg(Resource rate) { this.rate = rate; }
	}

	@Override
	protected ServiceResult internalDoService(AddRateArg arg) {
		if (arg.rate == null)
			return ServiceResult.error("Rate is missing!");

		try (StrolchTransaction tx = openArgOrUserTx(arg)) {
			tx.add(arg.rate);
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
	public AddRateArg getArgumentInstance() {
		return new AddRateArg();
	}
}
