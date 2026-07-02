package com.polarishb.pabal.workspace.domain.model;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceDomainArchitectureTest {

    @Test
    void workspace_domain_does_not_use_domain_time() throws IOException {
        Path sourceRoot = workspaceDomainSourceRoot();

        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            List<Path> sourcesUsingDomainTime = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(WorkspaceDomainArchitectureTest::containsInstantNow)
                    .toList();

            assertThat(sourcesUsingDomainTime).isEmpty();
        }
    }

    private static Path workspaceDomainSourceRoot() {
        Path projectRelativeSourceRoot = Path.of("src/main/java");
        if (Files.exists(projectRelativeSourceRoot)) {
            return projectRelativeSourceRoot;
        }
        return Path.of("pabal-workspace-domain/src/main/java");
    }

    private static boolean containsInstantNow(Path path) {
        try {
            return Files.readString(path).contains("Instant.now(");
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + path, exception);
        }
    }
}
