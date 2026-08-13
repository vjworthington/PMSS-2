package com.pennstatesoft.pmss.repository;

import com.pennstatesoft.pmss.model.Client;
import com.pennstatesoft.pmss.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserRepositoryTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final UserRepository repository = new UserRepository(jdbc);

    @Test
    void findByEmailDelegatesToQueryForObject() {
        User expected = new Client(1, "a@pennstatesoft.com", "h", "L", "F",
                "CLIENT", "2000-01-01", "F L", null, 0, null, null);
        when(jdbc.queryForObject(anyString(), any(RowMapper.class), eq("a@pennstatesoft.com")))
                .thenReturn(expected);

        assertSame(expected, repository.findByEmail("a@pennstatesoft.com"));
    }

    @Test
    void updateProfilePassesAllColumns() {
        byte[] image = {9, 9};

        repository.updateProfile("a@pennstatesoft.com", "New Name", "1999-09-09", image);

        verify(jdbc).update(anyString(), eq("New Name"), eq("1999-09-09"), any(), eq("a@pennstatesoft.com"));
    }

    @Test
    void updateProfileWithoutImagePassesThreeColumns() {
        repository.updateProfileWithoutImage("a@pennstatesoft.com", "New Name", "1999-09-09");

        verify(jdbc).update(anyString(), eq("New Name"), eq("1999-09-09"), eq("a@pennstatesoft.com"));
    }

    @Test
    void updateLoginFailureStringifiesTimestamps() {
        LocalDateTime lastFailed = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime lockedTo = LocalDateTime.of(2026, 1, 1, 10, 15);

        repository.updateLoginFailure(7L, 2, lastFailed, lockedTo);

        verify(jdbc).update(anyString(), eq(2), eq(lastFailed.toString()), eq(lockedTo.toString()), eq(7L));
    }

    @Test
    void updateLoginFailurePassesNullsWhenTimestampsMissing() {
        repository.updateLoginFailure(7L, 1, null, null);

        verify(jdbc).update(anyString(), eq(1), isNull(), isNull(), eq(7L));
    }

    @Test
    void resetLoginFailuresPassesEmail() {
        repository.resetLoginFailures("a@pennstatesoft.com");

        verify(jdbc).update(anyString(), eq("a@pennstatesoft.com"));
    }

    @Test
    void unlockAccountPassesEmail() {
        repository.unlockAccount("a@pennstatesoft.com");

        verify(jdbc).update(anyString(), eq("a@pennstatesoft.com"));
    }
}
