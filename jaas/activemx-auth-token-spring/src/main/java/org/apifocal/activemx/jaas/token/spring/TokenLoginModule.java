package org.apifocal.activemx.jaas.token.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import java.util.Map;

public class TokenLoginModule extends org.apifocal.activemx.jaas.token.TokenLoginModule {

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, sharedState, options);

        ApplicationContext context = SpringContextHolder.getInstance()
            .flatMap(SpringContextHolder::getApplicationContext)
            .orElseThrow(() -> new RuntimeException("Unable to locate Spring context"));

        validators.stream()
            .filter(ApplicationContextAware.class::isInstance)
            .map(ApplicationContextAware.class::cast)
            .forEach(validator -> validator.setApplicationContext(context));

        claimMappers.stream()
            .filter(ApplicationContextAware.class::isInstance)
            .map(ApplicationContextAware.class::cast)
            .forEach(validator -> validator.setApplicationContext(context));
    }
}
