package ch.eitchnet.pillowdesk.core.policy;

import ch.eitchnet.pillowdesk.core.search.StaySearch;
import li.strolch.model.Order;
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
		ZonedDateTime checkIn = stay.getDate(PARAM_CHECK_IN);
		ZonedDateTime checkOut = stay.getDate(PARAM_CHECK_OUT);

		if (checkOut.isBefore(checkIn.plusDays(1))) {
			throw new RuntimeException("Stay must be at least one night long!");
		}
	}

	public static void validateGuestName(Order stay) {
		if (StringHelper.isEmpty(stay.getName())) {
			throw new RuntimeException("Guest name must be set!");
		}
		if (StringHelper.isEmpty(stay.getString(PARAM_GUEST_NAME))) {
			throw new RuntimeException("Guest name must be set!");
		}
	}

	public static void validatePeopleCount(Order stay) {
		int adults = stay.getInteger(PARAM_ADULTS);
		int children = stay.getInteger(PARAM_CHILDREN);

		if (adults == 0 && children == 0) {
			throw new RuntimeException("At least one adult or child must be set!");
		}

		if (adults < 0 || children < 0) {
			throw new RuntimeException("People count cannot be negative!");
		}
	}

	public static void validateNoOverlap(StrolchTransaction tx, Order stay) {
		String roomId = stay.getString(BAG_RELATIONS, PARAM_ROOM);
		ZonedDateTime checkIn = stay.getDate(PARAM_CHECK_IN);
		ZonedDateTime checkOut = stay.getDate(PARAM_CHECK_OUT);

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
		stay.setString(PARAM_BOOKING_ID, id);
	}
}
