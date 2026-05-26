package ch.eitchnet.pillowdesk.core.policy;

import ch.eitchnet.pillowdesk.core.search.StaySearch;
import li.strolch.model.Order;
import li.strolch.model.parameter.DateParameter;
import li.strolch.model.parameter.IntegerParameter;
import li.strolch.model.parameter.StringParameter;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.utils.helper.StringHelper;

import java.time.ZonedDateTime;
import java.util.List;

import static ch.eitchnet.pillowdesk.core.model.ModelConstants.*;

public class StayValidationPolicy {

    public static void validate(StrolchTransaction tx, Order stay) {
        validateStayLength(stay);
        validateGuestName(stay);
        validatePeopleCount(stay);
        validateNoOverlap(tx, stay);
    }

    public static void validateStayLength(Order stay) {
        DateParameter checkInParam = stay.getParameter(BAG_PARAMETERS, PARAM_CHECK_IN, true);
        DateParameter checkOutParam = stay.getParameter(BAG_PARAMETERS, PARAM_CHECK_OUT, true);
        ZonedDateTime checkIn = checkInParam.getValueZdt();
        ZonedDateTime checkOut = checkOutParam.getValueZdt();

        if (checkOut.isBefore(checkIn.plusDays(1))) {
            throw new RuntimeException("Stay must be at least one night long!");
        }
    }

    public static void validateGuestName(Order stay) {
        if (StringHelper.isEmpty(stay.getName())) {
            throw new RuntimeException("Guest name must be set!");
        }
        StringParameter guestNameP = stay.getParameter(BAG_PARAMETERS, PARAM_GUEST_NAME, false);
        if (guestNameP == null || StringHelper.isEmpty(guestNameP.getValue())) {
            throw new RuntimeException("Guest name must be set!");
        }
    }

    public static void validatePeopleCount(Order stay) {
        IntegerParameter adultsP = stay.getParameter(BAG_PARAMETERS, PARAM_ADULTS, false);
        IntegerParameter childrenP = stay.getParameter(BAG_PARAMETERS, PARAM_CHILDREN, false);

        int adults = adultsP == null ? 0 : adultsP.getValue();
        int children = childrenP == null ? 0 : childrenP.getValue();

        if (adults == 0 && children == 0) {
            throw new RuntimeException("At least one adult or child must be set!");
        }

        if (adults < 0 || children < 0) {
            throw new RuntimeException("People count cannot be negative!");
        }
    }

    public static void validateNoOverlap(StrolchTransaction tx, Order stay) {
        String roomId = stay.getParameter(BAG_RELATIONS, PARAM_ROOM, true).getValue();
        DateParameter checkInParam = stay.getParameter(BAG_PARAMETERS, PARAM_CHECK_IN, true);
        ZonedDateTime checkIn = checkInParam.getValueZdt();
        DateParameter checkOutParam = stay.getParameter(BAG_PARAMETERS, PARAM_CHECK_OUT, true);
        ZonedDateTime checkOut = checkOutParam.getValueZdt();

        List<Order> overlaps = new StaySearch()
                .room(roomId)
                .dateRange(checkIn, checkOut)
                .search(tx)
                .toList();

        for (Order overlap : overlaps) {
            if (!overlap.getId().equals(stay.getId())) {
                throw new RuntimeException("Stay overlaps with existing stay " + overlap.getId() + " (" + overlap.getName() + ")");
            }
        }
    }

    public static void generateBookingId(StrolchTransaction tx, Order stay) {
        String id = StringHelper.getUniqueId();
        stay.setId(id);
        
        StringParameter bookingIdParam = stay.getParameter(BAG_PARAMETERS, PARAM_BOOKING_ID, false);
        if (bookingIdParam == null) {
            bookingIdParam = new StringParameter(PARAM_BOOKING_ID, "Booking ID", id);
            stay.addParameter(BAG_PARAMETERS, bookingIdParam);
        } else {
            bookingIdParam.setValue(id);
        }
    }
}
