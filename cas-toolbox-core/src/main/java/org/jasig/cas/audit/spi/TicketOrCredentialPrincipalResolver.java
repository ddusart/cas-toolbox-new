/*
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.cas.audit.spi;

import org.aspectj.lang.JoinPoint;

import com.github.inspektr.common.spi.PrincipalResolver;

import org.jasig.cas.authentication.Credential;
import org.jasig.cas.authentication.UsernamePasswordCredential;
import org.jasig.cas.ticket.ServiceTicket;
import org.jasig.cas.ticket.Ticket;
import org.jasig.cas.ticket.TicketGrantingTicket;
import org.jasig.cas.ticket.registry.TicketRegistry;
import org.jasig.cas.util.AopUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import javax.validation.constraints.NotNull;

/**
 * PrincipalResolver that can retrieve the username from either the Ticket or from the Credential.
 *
 * @author Scott Battaglia
 * @since 3.1.2
 *
 */
public final class TicketOrCredentialPrincipalResolver implements PrincipalResolver {

    @NotNull
    private final TicketRegistry ticketRegistry;

    public TicketOrCredentialPrincipalResolver(final TicketRegistry ticketRegistry) {
        this.ticketRegistry = ticketRegistry;
    }

    public String resolveFrom(final JoinPoint joinPoint, final Object retVal) {
        return resolveFromInternal(AopUtils.unWrapJoinPoint(joinPoint));
    }

    public String resolveFrom(final JoinPoint joinPoint, final Exception retVal) {
        return resolveFromInternal(AopUtils.unWrapJoinPoint(joinPoint));
    }

    public String resolve() {
        return UNKNOWN_USER;
    }

    protected String resolveFromInternal(final JoinPoint joinPoint) {
        final Object arg1 = joinPoint.getArgs()[0];
        //avoid audit:unknown
        if (arg1 instanceof Credential[]) {
            Credential[] credential;
            credential = new Credential[1];
            System.arraycopy(arg1, 0, credential, 0, 1);
            if (credential[0] instanceof UsernamePasswordCredential) {
                UsernamePasswordCredential[] userPassword = new UsernamePasswordCredential[1];
                System.arraycopy(credential, 0, userPassword, 0, 1);
                return userPassword[0].getUsername();
            } else {
                return credential[0].toString();
            }
        } else if (arg1 instanceof Credential) {
           return arg1.toString();
        } else if (arg1 instanceof String) {
            final Ticket ticket = this.ticketRegistry.getTicket((String) arg1);
            if (ticket instanceof ServiceTicket) {
                final ServiceTicket serviceTicket = (ServiceTicket) ticket;
                return serviceTicket.getGrantingTicket().getAuthentication().getPrincipal().getId();
            } else if (ticket instanceof TicketGrantingTicket) {
                final TicketGrantingTicket tgt = (TicketGrantingTicket) ticket;
                return tgt.getAuthentication().getPrincipal().getId();
            }
        } else {
            final SecurityContext securityContext = SecurityContextHolder.getContext();
            if (securityContext != null) {
                final Authentication authentication = securityContext.getAuthentication();

                if (authentication != null) {
                    return ((UserDetails) authentication.getPrincipal()).getUsername();
                }
            }
        }
        return UNKNOWN_USER;
    }
}
