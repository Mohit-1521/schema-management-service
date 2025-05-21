package com.flairlabs.workflow.services.schema_mangement_service.config;

import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

@NoArgsConstructor
public class TenantContext {

    private static final ThreadLocal<String> tenantIdentifier = new ThreadLocal<>();

    public static void setTenantIdentifier(String tenant) {
        tenantIdentifier.set(tenant);
    }

    @Nullable
    public static String getTenantIdentifier() {
        return tenantIdentifier.get();
    }

    public static void clear() {
        tenantIdentifier.remove();
    }


}
