package com.gympabon.dao;

import com.gympabon.model.Membership;
import com.gympabon.model.MembershipStatus;
import java.util.List;

/**
 * Data access for memberships.
 */
public interface MembershipDAO extends GenericDAO<Membership, Integer> {

    /**
     * Returns the membership history of a client, newest first.
     *
     * @param clientId identifier of the client
     * @return the client's memberships
     */
    List<Membership> findByClient(int clientId);

    /**
     * Returns the memberships that are in the given status.
     *
     * @param status status to filter by
     * @return the memberships ordered by start date
     */
    List<Membership> findByStatus(MembershipStatus status);
}
