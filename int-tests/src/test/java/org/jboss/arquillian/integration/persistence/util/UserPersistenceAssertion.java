package org.jboss.arquillian.integration.persistence.util;

import org.jboss.arquillian.integration.persistence.example.UserAccount;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class UserPersistenceAssertion {

    private final EntityManager em;

    public UserPersistenceAssertion(EntityManager em) {
        this.em = em;
        this.em.clear();
    }

    public void assertUserAccountsStored() {
        @SuppressWarnings("unchecked")
        List<UserAccount> savedUserAccounts = em.createQuery(Query.selectAllInJPQL(UserAccount.class)).getResultList();
        assertThat(savedUserAccounts).isNotEmpty();
    }

    public void assertNoUserAccountsStored() {
        @SuppressWarnings("unchecked")
        List<UserAccount> savedUserAccounts = em.createQuery(Query.selectAllInJPQL(UserAccount.class)).getResultList();
        assertThat(savedUserAccounts).isEmpty();
    }

}
