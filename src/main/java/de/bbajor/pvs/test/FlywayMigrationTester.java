package de.bbajor.pvs.test;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;

/**
 * Test utility to run Flyway migrations locally with H2 database
 * Usage: java -cp ... de.bbajor.pvs.test.FlywayMigrationTester jdbc:h2:mem:testdb sa ""
 */
public class FlywayMigrationTester {
    
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: FlywayMigrationTester <dbUrl> <user> <password>");
            System.err.println("Example: FlywayMigrationTester jdbc:h2:mem:testdb sa \"\"");
            System.exit(1);
        }
        
        String dbUrl = args[0];
        String user = args[1];
        String password = args[2];
        
        System.out.println("Testing Flyway migrations...");
        System.out.println("Database URL: " + dbUrl);
        System.out.println("User: " + user);
        System.out.println("Locations: classpath:db/migration");
        System.out.println();
        
        try {
            FluentConfiguration config = Flyway.configure()
                    .dataSource(dbUrl, user, password)
                    .locations("classpath:db/migration")
                    .validateMigrationNaming(true)
                    .baselineOnMigrate(true)
                    .validateOnMigrate(false)  // Skip validation for testing
                    .ignoreMigrationPatterns("*:pending");  // Ignore pending migrations for testing
            
            Flyway flyway = config.load();
            
            System.out.println("Running migrations...");
            var result = flyway.migrate();
            System.out.println("Migrations completed successfully! Applied: " + result.migrationsExecuted);
            
            System.out.println();
            System.out.println("Migration info:");
            var info = flyway.info();
            for (var migration : info.all()) {
                System.out.println("  " + migration.getVersion() + " - " + migration.getDescription() + 
                                 " (" + migration.getState() + ")");
            }
            
        } catch (Exception e) {
            System.err.println("Migration test failed!");
            e.printStackTrace();
            System.exit(1);
        }
    }
}

