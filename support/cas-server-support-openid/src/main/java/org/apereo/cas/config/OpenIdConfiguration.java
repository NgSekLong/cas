package org.apereo.cas.config;

import org.apereo.cas.CentralAuthenticationService;
import org.apereo.cas.authentication.AuthenticationContextValidator;
import org.apereo.cas.authentication.AuthenticationSystemSupport;
import org.apereo.cas.authentication.MultifactorTriggerSelectionStrategy;
import org.apereo.cas.authentication.adaptive.AdaptiveAuthenticationPolicy;
import org.apereo.cas.authentication.principal.ResponseBuilder;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.openid.authentication.principal.OpenIdServiceResponseBuilder;
import org.apereo.cas.support.openid.web.OpenIdProviderController;
import org.apereo.cas.support.openid.web.flow.OpenIdSingleSignOnAction;
import org.apereo.cas.support.openid.web.mvc.OpenIdValidateController;
import org.apereo.cas.support.openid.web.mvc.SmartOpenIdController;
import org.apereo.cas.support.openid.web.mvc.YadisController;
import org.apereo.cas.support.openid.web.support.DefaultOpenIdUserNameExtractor;
import org.apereo.cas.support.openid.web.support.OpenIdPostUrlHandlerMapping;
import org.apereo.cas.support.openid.web.support.OpenIdUserNameExtractor;
import org.apereo.cas.ticket.proxy.ProxyHandler;
import org.apereo.cas.ticket.registry.TicketRegistrySupport;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.validation.CasProtocolValidationSpecification;
import org.apereo.cas.validation.ServiceTicketValidationAuthorizersExecutionPlan;
import org.apereo.cas.web.AbstractDelegateController;
import org.apereo.cas.web.DelegatingController;
import org.apereo.cas.web.flow.resolver.CasDelegatingWebflowEventResolver;
import org.apereo.cas.web.flow.resolver.CasWebflowEventResolver;
import org.apereo.cas.web.support.ArgumentExtractor;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.openid4java.server.InMemoryServerAssociationStore;
import org.openid4java.server.ServerManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.View;
import org.springframework.webflow.execution.Action;

import java.util.Properties;

/**
 * This is {@link OpenIdConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Configuration("openidConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Slf4j
public class OpenIdConfiguration {

    @Autowired
    @Qualifier("adaptiveAuthenticationPolicy")
    private AdaptiveAuthenticationPolicy adaptiveAuthenticationPolicy;

    @Autowired
    @Qualifier("serviceTicketRequestWebflowEventResolver")
    private CasWebflowEventResolver serviceTicketRequestWebflowEventResolver;

    @Autowired
    @Qualifier("initialAuthenticationAttemptWebflowEventResolver")
    private CasDelegatingWebflowEventResolver initialAuthenticationAttemptWebflowEventResolver;

    @Autowired
    @Qualifier("cas3ServiceJsonView")
    private View cas3ServiceJsonView;

    @Autowired
    @Qualifier("casOpenIdServiceSuccessView")
    private View casOpenIdServiceSuccessView;

    @Autowired
    @Qualifier("casOpenIdServiceFailureView")
    private View casOpenIdServiceFailureView;

    @Autowired
    @Qualifier("casOpenIdAssociationSuccessView")
    private View casOpenIdAssociationSuccessView;

    @Autowired
    @Qualifier("proxy20Handler")
    private ProxyHandler proxy20Handler;


    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired
    @Qualifier("centralAuthenticationService")
    private ObjectProvider<CentralAuthenticationService> centralAuthenticationService;

    @Autowired
    @Qualifier("authenticationContextValidator")
    private AuthenticationContextValidator authenticationContextValidator;

    @Autowired
    @Qualifier("defaultAuthenticationSystemSupport")
    private ObjectProvider<AuthenticationSystemSupport> authenticationSystemSupport;

    @Autowired
    @Qualifier("cas20WithoutProxyProtocolValidationSpecification")
    private CasProtocolValidationSpecification cas20WithoutProxyProtocolValidationSpecification;

    @Autowired
    @Qualifier("defaultMultifactorTriggerSelectionStrategy")
    private MultifactorTriggerSelectionStrategy multifactorTriggerSelectionStrategy;

    @Autowired
    @Qualifier("servicesManager")
    private ObjectProvider<ServicesManager> servicesManager;

    @Autowired
    @Qualifier("defaultTicketRegistrySupport")
    private ObjectProvider<TicketRegistrySupport> ticketRegistrySupport;

    @Autowired
    @Qualifier("serviceValidationAuthorizers")
    private ServiceTicketValidationAuthorizersExecutionPlan validationAuthorizers;

    @Bean
    public AbstractDelegateController smartOpenIdAssociationController() {
        return new SmartOpenIdController(serverManager(), casOpenIdAssociationSuccessView);
    }

    @RefreshScope
    @Bean
    public ServerManager serverManager() {
        val manager = new ServerManager();
        manager.setOPEndpointUrl(casProperties.getServer().getLoginUrl());
        manager.setEnforceRpId(casProperties.getAuthn().getOpenid().isEnforceRpId());
        manager.setSharedAssociations(new InMemoryServerAssociationStore());
        LOGGER.info("Creating openid server manager with OP endpoint [{}]", casProperties.getServer().getLoginUrl());
        return manager;
    }

    @ConditionalOnMissingBean(name = "openIdServiceResponseBuilder")
    @Bean
    public ResponseBuilder openIdServiceResponseBuilder() {
        val openIdPrefixUrl = casProperties.getServer().getPrefix().concat("/openid");
        return new OpenIdServiceResponseBuilder(openIdPrefixUrl, serverManager(), centralAuthenticationService.getIfAvailable(), servicesManager.getIfAvailable());
    }


    @Bean
    @RefreshScope
    public YadisController yadisController() {
        return new YadisController();
    }


    @Bean
    @RefreshScope
    public OpenIdProviderController openIdProviderController() {
        return new OpenIdProviderController();
    }

    @Bean
    public Action openIdSingleSignOnAction() {
        return new OpenIdSingleSignOnAction(initialAuthenticationAttemptWebflowEventResolver, serviceTicketRequestWebflowEventResolver,
            adaptiveAuthenticationPolicy, defaultOpenIdUserNameExtractor(), ticketRegistrySupport.getIfAvailable());
    }

    @Bean
    public OpenIdUserNameExtractor defaultOpenIdUserNameExtractor() {
        return new DefaultOpenIdUserNameExtractor();
    }

    @Autowired
    @Bean
    public OpenIdPostUrlHandlerMapping openIdPostUrlHandlerMapping(@Qualifier("argumentExtractor") final ArgumentExtractor argumentExtractor) {
        val c = new OpenIdValidateController(cas20WithoutProxyProtocolValidationSpecification,
            authenticationSystemSupport.getIfAvailable(),
            servicesManager.getIfAvailable(),
            centralAuthenticationService.getIfAvailable(),
            proxy20Handler,
            argumentExtractor,
            multifactorTriggerSelectionStrategy,
            authenticationContextValidator,
            cas3ServiceJsonView,
            casOpenIdServiceSuccessView,
            casOpenIdServiceFailureView,
            casProperties.getAuthn().getMfa().getAuthenticationContextAttribute(),
            serverManager(),
            validationAuthorizers,
            casProperties.getSso().isRenewAuthnEnabled());

        val controller = new DelegatingController();
        controller.setDelegates(CollectionUtils.wrapList(smartOpenIdAssociationController(), c));

        val m = new OpenIdPostUrlHandlerMapping();
        m.setOrder(1);
        val mappings = new Properties();
        mappings.put("/login", controller);
        m.setMappings(mappings);
        return m;
    }
}
