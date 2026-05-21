package de.bbajor.pvs.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MigrationSqlTest {

    @Test
    void v31IndexesOnlyKnownColumns() throws IOException {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V31__optimize_database_performance.sql"));

        assertThat(sql)
                .contains("patient(birth)")
                .doesNotContain("patient(birth_date)")
                .doesNotContain("treatment_plan(status)");
    }
}
