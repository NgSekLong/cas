package org.apereo.cas.web.flow;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.web.flow.configurer.AbstractCasWebflowConfigurer;

import lombok.val;
import org.springframework.context.ApplicationContext;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.builder.support.FlowBuilderServices;

/**
 * The {@link TokenWebflowConfigurer} is responsible for
 * adjusting the CAS webflow context for token authentication support integration.
 *
 * @author Misagh Moayyed
 * @since 4.2
 */
public class TokenWebflowConfigurer extends AbstractCasWebflowConfigurer {

    public TokenWebflowConfigurer(final FlowBuilderServices flowBuilderServices,
                                  final FlowDefinitionRegistry loginFlowDefinitionRegistry,
                                  final ApplicationContext applicationContext,
                                  final CasConfigurationProperties casProperties) {
        super(flowBuilderServices, loginFlowDefinitionRegistry, applicationContext, casProperties);
    }

    @Override
    protected void doInitialize() {
        val flow = getLoginFlow();
        if (flow != null) {
            val actionState = createActionState(flow, "tokenAuthenticationCheck",
                createEvaluateAction("tokenAuthenticationAction"));
            actionState.getTransitionSet().add(createTransition(CasWebflowConstants.TRANSITION_ID_SUCCESS,
                CasWebflowConstants.STATE_ID_CREATE_TICKET_GRANTING_TICKET));
            actionState.getExitActionList().add(createEvaluateAction(CasWebflowConstants.ACTION_ID_CLEAR_WEBFLOW_CREDENTIALS));
            registerMultifactorProvidersStateTransitionsIntoWebflow(actionState);
            createStateDefaultTransition(actionState, getStartState(flow).getId());
            setStartState(flow, actionState);
        }
    }
}
