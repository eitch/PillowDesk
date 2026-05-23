package ch.eitchnet.pillowdesk.core.service;

import li.strolch.model.Order;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.service.api.AbstractService;
import li.strolch.service.api.ServiceArgument;
import li.strolch.service.api.ServiceResult;

public class RemoveStayService extends AbstractService<RemoveStayService.RemoveStayArg, ServiceResult> {

    public static class RemoveStayArg extends ServiceArgument {
        public String type;
        public String id;
    }

    @Override
    protected ServiceResult internalDoService(RemoveStayArg arg) {
        if (arg.id == null || arg.type == null)
            return ServiceResult.error("Stay ID or type is missing!");

        try (StrolchTransaction tx = openArgOrUserTx(arg)) {
            Order stay = tx.getOrderBy(arg.type, arg.id);
            if (stay == null)
                return ServiceResult.error("Stay with ID " + arg.id + " and type " + arg.type + " does not exist!");

            tx.remove(stay);
            tx.commitOnClose();
        }

        return ServiceResult.success();
    }

    @Override
    protected ServiceResult getResultInstance() {
        return new ServiceResult();
    }

    @Override
    public RemoveStayArg getArgumentInstance() {
        return new RemoveStayArg();
    }
}
