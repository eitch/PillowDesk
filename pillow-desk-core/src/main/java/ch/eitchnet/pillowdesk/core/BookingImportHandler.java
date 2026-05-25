package ch.eitchnet.pillowdesk.core;

import ch.eitchnet.pillowdesk.core.policy.StayValidationPolicy;
import li.strolch.agent.api.ComponentContainer;
import li.strolch.agent.api.StrolchAgent;
import li.strolch.agent.api.StrolchComponent;
import li.strolch.job.JobMode;
import li.strolch.job.StrolchJob;
import li.strolch.job.StrolchJobsHandler;
import li.strolch.model.Order;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.privilege.model.Certificate;
import li.strolch.privilege.model.PrivilegeContext;
import li.strolch.runtime.configuration.ComponentConfiguration;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static ch.eitchnet.pillowdesk.core.model.ModelConstants.*;
import static java.time.LocalDate.parse;

public class BookingImportHandler extends StrolchComponent {

	private static final String PROP_IMPORT_PATH = "importPath";
	private static final String DEFAULT_IMPORT_PATH = "import";

	public BookingImportHandler(ComponentContainer container, String componentName) {
		super(container, componentName);
	}

	@Override
	public void start() throws Exception {
		StrolchJobsHandler jobsHandler = getComponent(StrolchJobsHandler.class);
		StrolchJob job = jobsHandler.register(BookingImportJob.class);
		job.setMode(JobMode.Recurring);
		job.setDelay(10, TimeUnit.SECONDS, 1, TimeUnit.MINUTES);
		job.schedule();
		super.start();
	}

	public void importFiles() throws Exception {
		runAsAgent(ctx -> importFiles(ctx.getCertificate()));
	}

	public void importFiles(Certificate cert) {
		ComponentConfiguration configuration = getConfiguration();
		File importPath = configuration.getDataDir(PROP_IMPORT_PATH, DEFAULT_IMPORT_PATH, true);
		if (!importPath.exists() || !importPath.isDirectory()) {
			logger.warn("Import path {} does not exist or is not a directory.", importPath.getAbsolutePath());
			return;
		}

		File[] files = importPath.listFiles((dir, name) -> name.endsWith(".csv"));
		if (files == null) {
			return;
		}

		for (File file : files) {
			try {
				importFile(cert, file);
				moveToDone(file);
			} catch (Exception e) {
				logger.error("Failed to import file {}", file.getName(), e);
			}
		}
	}

	private void importFile(Certificate cert, File file) throws Exception {
		logger.info("Importing booking file {}", file.getAbsolutePath());

		CSVFormat format = CSVFormat.DEFAULT
				.builder()
				.setHeader()
				.setSkipHeaderRecord(true)
				.setIgnoreEmptyLines(true)
				.setTrim(true)
				.get();

		try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
		     CSVParser parser = format.parse(reader);
		     StrolchTransaction tx = openTx(cert, BookingImportHandler.class.getName(), false)) {

			DateTimeFormatter dateFormatterLong = DateTimeFormatter.ofPattern("dd.MM.yyyy");
			DateTimeFormatter dateFormatterShort = DateTimeFormatter.ofPattern("dd.MM.yy");

			for (CSVRecord record : parser) {
				try {
					processRecord(tx, record, dateFormatterLong, dateFormatterShort);
				} catch (Exception e) {
					logger.error("Failed to process record {} in file {}", record.getRecordNumber(), file.getName(), e);
				}
			}

			tx.commitOnClose();
		}
	}

	private void processRecord(StrolchTransaction tx, CSVRecord record, DateTimeFormatter dateFormatterLong,
			DateTimeFormatter dateFormatterShort) {
		String bookingId = record.get(0);
		String checkInStr = record.get(1);
		String checkOutStr = record.get(2);
		String guestName = record.get(3);
		String isAirBnbStr = record.get(4);
		String adultsStr = record.get(6);
		String childrenStr = record.get(7);

		if (bookingId.isEmpty() || checkInStr.isEmpty() || checkOutStr.isEmpty() || guestName.isEmpty()) {
			return;
		}

		LocalDate checkInDate = parse(checkInStr, getFormatter(dateFormatterLong, dateFormatterShort, checkInStr));
		LocalDate checkOutDate = parse(checkOutStr, getFormatter(dateFormatterLong, dateFormatterShort, checkOutStr));
		boolean isAirBnb = "Yes".equalsIgnoreCase(isAirBnbStr);
		int adults = adultsStr.isEmpty() ? 0 : Integer.parseInt(adultsStr);
		int children = childrenStr.isEmpty() ? 0 : Integer.parseInt(childrenStr);

		String orderId = "stay_" + bookingId;
		Order stay = tx.getOrderBy(TYPE_STAY, orderId);
		boolean exists = stay != null;
		if (!exists) {
			stay = tx.getOrderTemplate(TYPE_STAY);
			stay.setId(orderId);
		} else {
			stay = stay.getClone(true);
		}

		stay.setName(guestName);
		stay.getParameter(BAG_PARAMETERS, PARAM_BOOKING_ID, true).setValue(bookingId);
		stay.getParameter(BAG_PARAMETERS, PARAM_GUEST_NAME, true).setValue(guestName);
		stay.getParameter(BAG_PARAMETERS, PARAM_CHECK_IN, true).setValue(toDate(checkInDate));
		stay.getParameter(BAG_PARAMETERS, PARAM_CHECK_OUT, true).setValue(toDate(checkOutDate));
		stay.getParameter(BAG_PARAMETERS, PARAM_ADULTS, true).setValue(adults);
		stay.getParameter(BAG_PARAMETERS, PARAM_CHILDREN, true).setValue(children);
		stay.getParameter(BAG_PARAMETERS, PARAM_IS_AIR_BNB, true).setValue(isAirBnb);

		// Default relations
		stay.getParameter(BAG_RELATIONS, PARAM_ROOM, true).setValue("room_1");
		stay.getParameter(BAG_RELATIONS, PARAM_RATE, true).setValue(isAirBnb ? "rate_airbnb" : "rate_standard");

		StayValidationPolicy.validateNoOverlap(tx, stay);

		if (exists)
			tx.update(stay);
		else
			tx.add(stay);

		logger.info("Imported booking: {} {} {} {}", stay.getId(), guestName, checkInDate, checkOutDate);
	}

	private static DateTimeFormatter getFormatter(DateTimeFormatter dateFormatterLong,
			DateTimeFormatter dateFormatterShort, String checkInStr) {
		return checkInStr.length() == 10 ? dateFormatterLong : dateFormatterShort;
	}

	private Date toDate(LocalDate localDate) {
		return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	private void moveToDone(File file) {
		File doneDir = new File(file.getParentFile(), "done");
		if (!doneDir.exists() && !doneDir.mkdirs()) {
			logger.error("Failed to create done directory {}", doneDir.getAbsolutePath());
			return;
		}
		File dest = new File(doneDir, file.getName());
		if (file.renameTo(dest)) {
			logger.info("Moved imported file to {}", dest.getAbsolutePath());
		} else {
			logger.error("Failed to move imported file to {}", dest.getAbsolutePath());
		}
	}

	public static class BookingImportJob extends StrolchJob {

		public BookingImportJob(StrolchAgent agent, String id, String name, JobMode jobMode) {
			super(agent, id, name, jobMode);
		}

		@Override
		protected void execute(PrivilegeContext ctx) throws Exception {
			getComponent(BookingImportHandler.class).importFiles(ctx.getCertificate());
		}
	}
}
