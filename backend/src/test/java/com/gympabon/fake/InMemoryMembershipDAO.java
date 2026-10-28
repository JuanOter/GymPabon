package com.gympabon.fake;

import com.gympabon.dao.MembershipDAO;
import com.gympabon.model.Membership;
import com.gympabon.model.MembershipStatus;
import java.util.Comparator;
import java.util.List;

/** In-memory {@link MembershipDAO} for unit tests. */
public class InMemoryMembershipDAO extends InMemoryDAO<Membership> implements MembershipDAO {

    @Override
    protected int idOf(Membership membership) {
        return membership.getId();
    }

    @Override
    protected void assignId(Membership membership, int id) {
        membership.setId(id);
    }

    @Override
    public List<Membership> findByClient(int clientId) {
        return rows.values().stream().filter(m -> m.getClient().getId() == clientId)
                .sorted(Comparator.comparing(Membership::getStartDate).reversed()).toList();
    }

    @Override
    public List<Membership> findByStatus(MembershipStatus status) {
        return rows.values().stream().filter(m -> m.getStatus() == status)
                .sorted(Comparator.comparing(Membership::getStartDate)).toList();
    }
}
