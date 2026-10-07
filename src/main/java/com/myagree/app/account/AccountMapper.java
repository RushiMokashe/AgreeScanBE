package com.myagree.app.account;

import com.myagree.app.account.dto.CurrentUserResponse;

final class AccountMapper {

    private AccountMapper() {
    }

    static Account toAccount(User user) {
        return new Account(user.getId(), user.getName(), user.getPhone(), user.getEmail(),
                user.getRoles().stream().sorted().toList(), user.isActive(), user.getPreferredLanguage(),
                user.getFarmerId(), user.getOwnerId(), user.getShopId(), user.getCreatedAt(), user.getLastLoginAt());
    }

    static CurrentUserResponse toCurrentUserResponse(Account account) {
        return new CurrentUserResponse(account.id(), account.name(), account.phone(), account.email(), account.roles(),
                account.preferredLanguage(), account.farmerId(), account.ownerId(), account.shopId());
    }
}
