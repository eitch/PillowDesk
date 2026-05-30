# 🛋️ PillowDesk

PillowDesk is a lightweight booking and stay management system built on the [Strolch](https://strolch.li) framework. It is designed to manage hotel-like stays, rooms, and rates, providing essential reporting on revenue and occupancy.

## 🚀 Features

- **Stay Management**: Complete lifecycle management of guest stays (Add, Edit, Remove).
- **Rate Overrides**: Define date-specific rates that override the default base price for any given day. Supports managing overrides across multiple rate types.
- **Guest Search**: Filter stays by guest name, check-in, and check-out dates.
- **Room & Rate Relations**: Associate stays with specific rooms and pricing rates.
- **Reporting & Analytics**:
  - **Daily & Monthly Summary**: Detailed breakdown of pro-rated daily revenue and tourist taxes.
  - **Yearly Performance**: Monthly overview of revenue, taxes, and occupancy rates.
  - **Booking Distribution**: Tracks Airbnb vs. Direct bookings.
- **Dynamic Pricing**: Stay cost calculation automatically incorporates rate overrides for specific dates, allowing for seasonal or event-based pricing adjustments.
- **Authentication**: Secure access powered by Strolch's privilege system.

## 🛠️ Tech Stack

- **Backend**: Java 25, Strolch Framework, Jakarta REST (Jersey).
- **Frontend**: Vanilla HTML5, CSS3, and JavaScript (SPA architecture).
- **Build Tool**: Maven.

## 📁 Project Structure

- `pillow-desk-core`: Contains the domain model, business logic, services, and policies.
- `pillow-desk-rest`: Provides the Jakarta REST API endpoints for the web interface.
- `pillow-desk-web`: The web application frontend (packaged as WAR).
- `runtime`: Contains Strolch configuration and initial data model (Rooms, Rates, etc.).

## 🏁 Getting Started

### Prerequisites

- Java 25 or higher.
- Maven 3.9+.

### Build

To build the project, run:

```bash
mvn clean install
```

### Running the Application

PillowDesk is a Strolch-based web application. It is packaged as a WAR file and can be deployed to any Jakarta EE compatible servlet container (e.g., Tomcat, Jetty).

For development, the project is configured with a local development profile that uses the runtime configuration in the `runtime` directory.

## 🌐 Links

- **Website**: [https://pillow-desk.eitchnet.ch](https://pillow-desk.eitchnet.ch)

## 📄 License

PillowDesk is licensed under the [GNU Affero General Public License v3.0](LICENSE).

Copyright © 2026 eitch - Robert von Burg
