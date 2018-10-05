package org.apereo.cas.authentication;

import org.apereo.cas.configuration.model.support.mfa.MultifactorAuthenticationProviderBypassProperties;
import org.apereo.cas.services.MultifactorAuthenticationProvider;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.HttpUtils;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

/**
 * This is {@link RestMultifactorAuthenticationProviderBypass}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@Slf4j
public class RestMultifactorAuthenticationProviderBypass extends AbstractMultifactorAuthenticationProviderBypass {

    private static final long serialVersionUID = -7553888418344342672L;

    public RestMultifactorAuthenticationProviderBypass(final MultifactorAuthenticationProviderBypassProperties bypassProperties) {
        super(bypassProperties);
    }

    @Override
    public boolean shouldMultifactorAuthenticationProviderExecute(final Authentication authentication, final RegisteredService registeredService,
                                                                  final MultifactorAuthenticationProvider provider,
                                                                  final HttpServletRequest request) {
        try {
            val principal = authentication.getPrincipal();
            val rest = bypassProperties.getRest();
            LOGGER.debug("Evaluating multifactor authentication bypass properties for principal [{}], "
                    + "service [{}] and provider [{}] via REST endpoint [{}]",
                principal.getId(), registeredService, provider, rest.getUrl());

            val parameters = CollectionUtils.wrap("principal", principal.getId(), "provider", provider.getId());
            if (registeredService != null) {
                parameters.put("service", registeredService.getServiceId());
            }

            val response = HttpUtils.execute(rest.getUrl(), rest.getMethod(),
                rest.getBasicAuthUsername(), rest.getBasicAuthPassword(), parameters, new HashMap<>());
            val shouldExecute = response.getStatusLine().getStatusCode() == HttpStatus.ACCEPTED.value();
            if (shouldExecute) {
                updateAuthenticationToForgetBypass(authentication, provider, principal);
            } else {
                LOGGER.info("REST bypass endpoint response determined [{}] would be passed for [{}]", principal.getId(), provider.getId());
                updateAuthenticationToRememberBypass(authentication, provider, principal);
            }
            return shouldExecute;
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return true;
    }
}
