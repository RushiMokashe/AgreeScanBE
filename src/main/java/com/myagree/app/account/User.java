package com.myagree.app.account;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import org.jspecify.annotations.Nullable;

import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.security.Role;

/**
 * Someone who signs in with a phone number and password. The farmer and rental features link the user's
 * profiles by id, so this feature depends on no other.
 */
@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = PhoneNumbers.LENGTH)
    private String phone;

    @Column(nullable = false, length = Account.NAME_MAX_LENGTH)
    private String name;

    @Column(length = Account.EMAIL_MAX_LENGTH)
    private @Nullable String email;

    @Column(nullable = false)
    private String passwordHash;

    @ElementCollection
    @CollectionTable(name = "app_user_role", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = EnumSet.noneOf(Role.class);

    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language preferredLanguage;

    private @Nullable Long farmerId;
    private @Nullable Long ownerId;
    private @Nullable Long shopId;

    @Column(nullable = false)
    private Instant createdAt;

    private @Nullable Instant lastLoginAt;

    protected User() {
    }

    User(String phone, String name, @Nullable String email, String passwordHash, Set<Role> roles,
         Language preferredLanguage, Instant createdAt) {
        this.phone = phone;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = EnumSet.copyOf(roles);
        this.active = true;
        this.preferredLanguage = preferredLanguage;
        this.createdAt = createdAt;
    }

    boolean hasRole(Role role) {
        return roles.contains(role);
    }

    void rename(String name) {
        this.name = name;
    }

    void changeEmail(@Nullable String email) {
        this.email = email;
    }

    void changePreferredLanguage(Language preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    void replaceRoles(Set<Role> roles) {
        this.roles.clear();
        this.roles.addAll(roles);
    }

    void changeActive(boolean active) {
        this.active = active;
    }

    void recordLogin(Instant at) {
        this.lastLoginAt = at;
    }

    void linkFarmerProfile(long farmerId) {
        this.farmerId = farmerId;
    }

    void linkOwnerProfile(long ownerId) {
        this.ownerId = ownerId;
    }

    void linkShop(long shopId) {
        this.shopId = shopId;
    }

    public Long getId() {
        return id;
    }

    public String getPhone() {
        return phone;
    }

    public String getName() {
        return name;
    }

    public @Nullable String getEmail() {
        return email;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    public Set<Role> getRoles() {
        return Set.copyOf(roles);
    }

    public boolean isActive() {
        return active;
    }

    public Language getPreferredLanguage() {
        return preferredLanguage;
    }

    public @Nullable Long getFarmerId() {
        return farmerId;
    }

    public @Nullable Long getOwnerId() {
        return ownerId;
    }

    public @Nullable Long getShopId() {
        return shopId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public @Nullable Instant getLastLoginAt() {
        return lastLoginAt;
    }
}
