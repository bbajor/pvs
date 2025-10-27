# PVS (Praxis-Verwaltungs-System)

## Lizenzierung
- Code steht unter Business Source License 1.1 (BUSL-1.1) mit Parametern in `LICENSE.md`.
- Interner Betrieb in eigenen Praxen erlaubt; Angebot als SaaS nur mit separater Hosting-Lizenz (`HOSTING-LIZENZ-DE.md`).
- Change Date: 2028-10-27 → Wechsel auf Apache-2.0.

## Prerequisites

- Java 17 or higher
- Maven 3.8+
- OpenSC for eGK card reading functionality
- eGK-Tool (available from your KV/gematik)

## Getting Started

1. Clone the repository:
```bash
git clone https://github.com/bbajor/pvs.git
cd pvs
```

2. Install OpenSC:
   - Download from [OpenSC Project](https://github.com/OpenSC/OpenSC/releases)
   - Install with default settings
   - Ensure the card reader is connected and recognized

3. Configure application.properties:
   Create or modify `src/main/resources/application.properties`:
```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/pvs
spring.datasource.username=your_username
spring.datasource.password=your_password

# eGK Tool Configuration
egk.tool-path=C:/Path/To/Your/egk-tool.exe

# Server Configuration
server.port=8080
```

## Development

To start the application in development mode:

```bash
mvn spring-boot:run
```

Or run the `Application` class from your IDE.

## Production Build

To build for production:

```bash
mvn clean package -Pproduction
```

## Troubleshooting

### eGK Card Reading Issues
- Verify OpenSC installation: `opensc-tool --version`
- Check if card reader is recognized: `opensc-tool --list-readers`
- Ensure egk-tool.exe path is correctly set in application.properties
- Check system PATH includes OpenSC binary directory

### Project Structure
```
pvs/
├── src/
│   ├── main/
│   │   ├── java/de/bbajor/pvs/
│   │   │   ├── base/        # Base entities and services
│   │   │   ├── config/      # Configuration classes
│   │   │   └── ui/         # Vaadin UI components
│   │   └── resources/
│   │       └── application.properties
│   └── test/
└── pom.xml
```

## Links
- [GitHub Repository](https://github.com/bbajor/pvs)
- [Hosting-Lizenz (DE)](./HOSTING-LIZENZ-DE.md)
- [BUSL 1.1 Text](https://mariadb.com/bsl11/)
- [Vaadin Documentation](https://vaadin.com/docs)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
