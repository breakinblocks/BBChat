package com.breakinblocks.bblink.core.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelayServiceImplTest {
    @TempDir
    Path tempDir;

    @Test
    void databasePathMayBeAFolder() {
        Path expected = tempDir.resolve(RelayServiceImpl.SHARED_DATABASE_FILE_NAME);
        assertEquals(expected, RelayServiceImpl.resolveDatabaseFile(tempDir.toString()));
        assertEquals(expected, RelayServiceImpl.resolveDatabaseFile(tempDir + "/"));
    }

    @Test
    void databasePathMayBeAFile() {
        Path file = tempDir.resolve("chat.db");
        assertEquals(file, RelayServiceImpl.resolveDatabaseFile(file.toString()));
    }

    @Test
    void missingFolderWithTrailingSlashIsTreatedAsFolder() {
        Path folder = tempDir.resolve("not-created-yet");
        assertEquals(folder.resolve(RelayServiceImpl.SHARED_DATABASE_FILE_NAME),
                RelayServiceImpl.resolveDatabaseFile(folder + "/"));
    }
}
