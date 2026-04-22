package com.sophie.aac.profile.service;

import com.sophie.aac.auth.domain.CaregiverAccountEntity;
import com.sophie.aac.auth.domain.Role;
import com.sophie.aac.auth.repository.CaregiverAccountProfileRepository;
import com.sophie.aac.auth.repository.CaregiverAccountRepository;
import com.sophie.aac.auth.repository.UserAccountProfileRepository;
import com.sophie.aac.auth.util.AuthContext;
import com.sophie.aac.common.web.UnauthorizedException;
import com.sophie.aac.profile.domain.UserProfileEntity;
import com.sophie.aac.profile.repository.UserProfileRepository;
import com.sophie.aac.suggestions.domain.LocationCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProfileSetupServiceTest {

  private UserProfileRepository profileRepo;
  private CaregiverAccountRepository accountRepo;
  private CaregiverAccountProfileRepository accountProfileRepo;
  private UserAccountProfileRepository userAccountProfileRepo;
  private AuthContext authContext;
  private ProfileSetupService service;

  @BeforeEach
  void setUp() {
    profileRepo = mock(UserProfileRepository.class);
    accountRepo = mock(CaregiverAccountRepository.class);
    accountProfileRepo = mock(CaregiverAccountProfileRepository.class);
    userAccountProfileRepo = mock(UserAccountProfileRepository.class);
    authContext = mock(AuthContext.class);

    service = new ProfileSetupService(
        profileRepo, accountRepo, accountProfileRepo, userAccountProfileRepo, authContext
    );
  }

  @Test
  void createProfile_requires_authenticated_role() {
    when(authContext.currentRole()).thenReturn(null);

    assertThatThrownBy(() -> service.createProfile("Sam", "Hey Sam"))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessageContaining("Sign in");
  }

  @Test
  void createProfile_for_user_creates_and_links_user_profile() {
    UUID userId = UUID.randomUUID();
    when(authContext.currentRole()).thenReturn(Role.PARENT);
    when(authContext.currentUserId()).thenReturn(userId);
    when(profileRepo.save(any(UserProfileEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    UserProfileEntity created = service.createProfile("  Alex  ", "  Hi  ");

    assertThat(created.getId()).isNotNull();
    assertThat(created.getDisplayName()).isEqualTo("Alex");
    assertThat(created.getWakeName()).isEqualTo("Hi");
    assertThat(created.getDefaultLocation()).isEqualTo(LocationCategory.HOME);
    assertThat(created.getPreferredIconSize()).isEqualTo("large");
    verify(userAccountProfileRepo, times(1)).save(any());
    verify(accountProfileRepo, never()).save(any());
  }

  @Test
  void createProfile_for_caregiver_account_creates_and_links_account_profile() {
    UUID accountId = UUID.randomUUID();
    CaregiverAccountEntity account = new CaregiverAccountEntity();
    account.setId(accountId);
    account.setRole(Role.CARER);

    when(authContext.currentRole()).thenReturn(Role.CARER);
    when(authContext.currentUserId()).thenReturn(null);
    when(accountRepo.findByRole(Role.CARER)).thenReturn(Optional.of(account));
    when(profileRepo.save(any(UserProfileEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    UserProfileEntity created = service.createProfile(" ", " ");

    assertThat(created.getDisplayName()).isEqualTo("New User");
    assertThat(created.getWakeName()).isEqualTo("Hey");
    verify(accountProfileRepo, times(1)).save(any());
    verify(userAccountProfileRepo, never()).save(any());
  }

  @Test
  void createProfile_for_caregiver_throws_when_account_missing() {
    when(authContext.currentRole()).thenReturn(Role.CARER);
    when(authContext.currentUserId()).thenReturn(null);
    when(accountRepo.findByRole(Role.CARER)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.createProfile("Sam", "Wake"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Account not found");
  }
}
