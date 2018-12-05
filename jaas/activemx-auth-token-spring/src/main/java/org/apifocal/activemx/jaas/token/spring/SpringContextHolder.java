package org.apifocal.activemx.jaas.token.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Optional;

public class SpringContextHolder implements ApplicationContextAware, InitializingBean {

    private static SpringContextHolder INSTANCE;

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public Optional<ApplicationContext> getApplicationContext() {
        return Optional.ofNullable(applicationContext);
    }

    public static Optional<SpringContextHolder> getInstance() {
        return Optional.ofNullable(INSTANCE);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        INSTANCE = this;
    }
}
