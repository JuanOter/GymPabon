package com.gympabon.service;

import com.gympabon.dao.ClientDAO;
import com.gympabon.dao.MembershipDAO;
import com.gympabon.dao.PlanDAO;
import com.gympabon.dao.TrainerDAO;
import com.gympabon.exception.BusinessRuleException;
import com.gympabon.exception.NotFoundException;
import com.gympabon.model.Client;
import com.gympabon.model.Membership;
import com.gympabon.model.MembershipDuration;
import com.gympabon.model.MembershipStatus;
import com.gympabon.model.Plan;
import com.gympabon.model.Trainer;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Business rules for selling, renewing and cancelling memberships.
 */
@Service
public class MembershipService {

    private static final Set<MembershipStatus> CURRENT_STATUSES =
            Set.of(MembershipStatus.PENDING_PAYMENT, MembershipStatus.SCHEDULED, MembershipStatus.ACTIVE);

    private final MembershipDAO membershipDAO;
    private final ClientDAO clientDAO;
    private final PlanDAO planDAO;
    private final TrainerDAO trainerDAO;

    public MembershipService(MembershipDAO membershipDAO, ClientDAO clientDAO, PlanDAO planDAO,
                             TrainerDAO trainerDAO) {
        this.membershipDAO = membershipDAO;
        this.clientDAO = clientDAO;
        this.planDAO = planDAO;
        this.trainerDAO = trainerDAO;
    }

    /**
     * Creates a new membership pending payment. The enrollment fee is only
     * charged on the client's first paid membership, and a Premium plan needs
     * a trainer with a free slot.
     *
     * @param clientId  identifier of the client
     * @param planId    identifier of the plan
     * @param duration  duration bought
     * @param startDate first day of the membership, today or later
     * @param trainerId identifier of the trainer for Premium plans, otherwise null
     * @return the saved membership
     * @throws NotFoundException     if the client, the plan or the trainer do not exist
     * @throws BusinessRuleException if a business rule is not met
     */
    public Membership createMembership(int clientId, int planId, MembershipDuration duration, LocalDate startDate,
                                       Integer trainerId) {
        Client client = loadClientWithHistory(clientId);
        if (hasCurrentMembership(client)) {
            throw new BusinessRuleException(
                    "El cliente ya tiene una membresía vigente o pendiente de pago; use la renovación");
        }
        if (startDate == null || startDate.isBefore(LocalDate.now())) {
            throw new BusinessRuleException("La fecha de inicio no puede ser anterior a hoy");
        }
        return buildAndSave(client, planId, duration, startDate, trainerId);
    }

    /**
     * Creates the renewal of the active membership. It starts the day after
     * the current one ends, never charges the enrollment fee and may change
     * the plan.
     *
     * @param clientId  identifier of the client
     * @param planId    identifier of the plan of the renewal
     * @param duration  duration bought
     * @param trainerId identifier of the trainer for Premium plans, otherwise null
     * @return the saved renewal
     * @throws NotFoundException     if the client, the plan or the trainer do not exist
     * @throws BusinessRuleException if the client has no active membership or
     *                               already has a renewal
     */
    public Membership renewMembership(int clientId, int planId, MembershipDuration duration, Integer trainerId) {
        Client client = loadClientWithHistory(clientId);
        boolean hasRenewal = client.getMemberships().stream()
                .anyMatch(m -> m.getStatus() == MembershipStatus.SCHEDULED
                        || m.getStatus() == MembershipStatus.PENDING_PAYMENT);
        if (hasRenewal) {
            throw new BusinessRuleException("El cliente ya tiene una renovación pendiente de pago o programada");
        }
        Membership current = client.getMemberships().stream()
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .max(Comparator.comparing(Membership::getEndDate))
                .orElseThrow(() -> new BusinessRuleException(
                        "El cliente no tiene una membresía activa para renovar; registre una nueva"));
        return buildAndSave(client, planId, duration, current.getEndDate().plusDays(1), trainerId);
    }

    /**
     * Calculates the value to pay before confirming the sale, including the
     * enrollment fee when it applies.
     *
     * @param clientId identifier of the client
     * @param planId   identifier of the plan
     * @param duration duration to quote
     * @return the total in pesos
     */
    public double quote(int clientId, int planId, MembershipDuration duration) {
        Client client = loadClientWithHistory(clientId);
        Plan plan = findPlan(planId);
        requireDuration(duration);
        return new Membership(client, plan, null, duration, LocalDate.now()).getTotalAmount();
    }

    /**
     * Cancels a membership with its reason. The trainer slot is freed because
     * the membership is no longer current. Only the administrator uses it.
     *
     * @param membershipId identifier of the membership
     * @param reason       why it is cancelled
     * @throws NotFoundException     if the membership does not exist
     * @throws BusinessRuleException if the reason is empty or the membership already ended
     */
    public void cancelMembership(int membershipId, String reason) {
        Membership membership = findById(membershipId);
        membership.cancel(reason);
        membershipDAO.update(membership);
    }

    /**
     * Updates the status of every current membership for the given day:
     * scheduled ones that start become active and active ones that ended
     * become expired.
     *
     * @param today reference date
     * @return how many memberships changed their status
     */
    public int refreshStatuses(LocalDate today) {
        int changed = 0;
        for (Membership membership : membershipDAO.findAll()) {
            MembershipStatus before = membership.getStatus();
            if (before == MembershipStatus.CANCELLED || before == MembershipStatus.EXPIRED) {
                continue;
            }
            membership.updateStatus(today);
            if (membership.getStatus() != before) {
                membershipDAO.update(membership);
                changed++;
            }
        }
        return changed;
    }

    /**
     * Returns the membership history of a client, newest first.
     *
     * @param clientId identifier of the client
     * @return the client's memberships
     */
    public List<Membership> findByClient(int clientId) {
        return membershipDAO.findByClient(clientId);
    }

    /**
     * Finds a membership by its identifier.
     *
     * @param membershipId identifier of the membership
     * @return the membership
     * @throws NotFoundException if the membership does not exist
     */
    public Membership findById(int membershipId) {
        Membership membership = membershipDAO.findById(membershipId);
        if (membership == null) {
            throw new NotFoundException("La membresía no existe");
        }
        return membership;
    }

    private Membership buildAndSave(Client client, int planId, MembershipDuration duration, LocalDate startDate,
                                    Integer trainerId) {
        requireDuration(duration);
        Plan plan = findPlan(planId);
        Trainer trainer = resolveTrainer(plan, trainerId);
        Membership membership = new Membership(client, plan, trainer, duration, startDate);
        if (trainer != null) {
            trainer.assignMembership(membership);
        }
        membershipDAO.save(membership);
        client.addMembership(membership);
        return membership;
    }

    private Client loadClientWithHistory(int clientId) {
        Client client = clientDAO.findById(clientId);
        if (client == null) {
            throw new NotFoundException("El cliente no existe");
        }
        if (!client.isActive()) {
            throw new BusinessRuleException("El cliente está inactivo");
        }
        client.getMemberships().addAll(membershipDAO.findByClient(clientId));
        return client;
    }

    private boolean hasCurrentMembership(Client client) {
        return client.getMemberships().stream()
                .anyMatch(m -> CURRENT_STATUSES.contains(m.getStatus()));
    }

    private Plan findPlan(int planId) {
        Plan plan = planDAO.findById(planId);
        if (plan == null) {
            throw new NotFoundException("El plan no existe");
        }
        return plan;
    }

    private Trainer resolveTrainer(Plan plan, Integer trainerId) {
        if (!plan.includesTrainer()) {
            if (trainerId != null) {
                throw new BusinessRuleException("Solo el plan Premium incluye entrenador");
            }
            return null;
        }
        if (trainerId == null) {
            throw new BusinessRuleException("El plan Premium requiere elegir un entrenador");
        }
        Trainer trainer = trainerDAO.findById(trainerId);
        if (trainer == null) {
            throw new NotFoundException("El entrenador no existe");
        }
        return trainer;
    }

    private void requireDuration(MembershipDuration duration) {
        if (duration == null) {
            throw new BusinessRuleException("La duración es obligatoria");
        }
    }
}
