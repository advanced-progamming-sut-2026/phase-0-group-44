package persistence;

import controller.LoginMenuController;
import model.Store;
import model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonUserRepository;
import service.Sha256PasswordService;
import service.UserService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class PasswordMigrationTest {
    @TempDir Path tempDir;

    @BeforeEach
    void reset() {
        Store.setUsers(new ArrayList<>());
        Store.setLoggedInUser(null);
    }

    @Test
    void compatiblePlaintextIsHashedOnceAndAtomicallyRewritten() throws Exception {
        Path save = tempDir.resolve("users.json");
        Files.writeString(save, """
                {"version":1,"users":[{"username":"legacy",
                "hashOfPassword":"Secret1!","nickname":"Legacy",
                "email":"legacy@example.com","gender":"male"}]}
                """);
        UserService users = new UserService(new JsonUserRepository(save), Clock.systemUTC());
        assertTrue(users.loadUsers().getStatus());
        User migrated = users.findByUsername("legacy");
        assertTrue(Sha256PasswordService.isStoredHash(migrated.getHashOfPassword()));
        assertEquals(User.hashPassword("Secret1!"), migrated.getHashOfPassword());
        String rewritten = Files.readString(save);
        assertFalse(rewritten.contains("Secret1!"));
        assertTrue(rewritten.contains("\"version\": 2"));

        String digest = migrated.getHashOfPassword();
        users.loadUsers();
        assertEquals(digest, users.findByUsername("legacy").getHashOfPassword());
        assertTrue(new LoginMenuController(users, new Sha256PasswordService())
                .login("legacy", "Secret1!", false).getStatus());
    }
}
