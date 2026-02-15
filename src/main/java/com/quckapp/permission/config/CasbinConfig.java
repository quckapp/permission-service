package com.quckapp.permission.config;

import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.SyncedEnforcer;
import org.casbin.jcasbin.model.Model;
import org.casbin.jcasbin.persist.Adapter;
import org.casbin.adapter.JDBCAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
@Slf4j
public class CasbinConfig {

    @Value("classpath:casbin/rbac_model.conf")
    private Resource modelResource;

    @Bean
    public Adapter casbinAdapter(DataSource dataSource) throws Exception {
        return new JDBCAdapter(dataSource);
    }

    @Bean
    public SyncedEnforcer enforcer(Adapter adapter) throws Exception {
        // Read model from resource (works both in IDE and JAR)
        String modelText;
        try (InputStream is = modelResource.getInputStream()) {
            modelText = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
        }

        // Create model from text
        Model model = new Model();
        model.loadModelFromText(modelText);

        // Create enforcer with model and adapter
        SyncedEnforcer enforcer = new SyncedEnforcer(model, adapter);
        enforcer.enableAutoSave(true);
        log.info("Casbin SyncedEnforcer initialized with RBAC model");
        return enforcer;
    }
}
