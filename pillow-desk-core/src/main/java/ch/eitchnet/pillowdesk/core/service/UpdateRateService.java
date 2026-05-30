package ch.eitchnet.pillowdesk.core.service;

import li.strolch.model.Resource;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.service.api.AbstractService;
import li.strolch.service.api.ServiceArgument;
import li.strolch.service.api.ServiceResult;

public class UpdateRateService extends AbstractService<UpdateRateService.UpdateRateArg, ServiceResult> {

	public static class UpdateRateArg extends ServiceArgument {
		public Resource rate;
		public UpdateRateArg() {}
		public UpdateRateArg(Resource rate) { this.rate = rate; }
	}

	@Override
	protected ServiceResult internalDoService(UpdateRateArg arg) {
		if (arg.rate == null)
			return ServiceResult.error("Rate is missing!");

		try (StrolchTransaction tx = openArgOrUserTx(arg)) {
			tx.update(arg.rate);
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
	public UpdateRateArg getArgumentInstance() {
		return new UpdateRateArg();
	}
}
