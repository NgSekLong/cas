package org.apereo.cas.ticket.support;

import org.apereo.cas.ticket.ExpirationPolicy;
import org.apereo.cas.ticket.TicketState;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.val;

import java.util.UUID;

/**
 * This is an {@link AbstractCasExpirationPolicy}
 * that serves as the root parent for all CAS expiration policies
 * and exposes a few internal helper methods to children can access
 * to objects like the request, etc.
 *
 * @author Misagh Moayyed
 * @since 4.1
 */
@Getter
@Setter
@EqualsAndHashCode
public abstract class AbstractCasExpirationPolicy implements ExpirationPolicy {

    private static final long serialVersionUID = 8042104336580063690L;

    private String name;

    public AbstractCasExpirationPolicy() {
        this.name = this.getClass().getSimpleName() + '-' + UUID.randomUUID().toString();
    }

    @Override
    public boolean isExpired(final TicketState ticketState) {
        val tgt = ticketState.getTicketGrantingTicket();
        return tgt != null && tgt.isExpired();
    }
}
