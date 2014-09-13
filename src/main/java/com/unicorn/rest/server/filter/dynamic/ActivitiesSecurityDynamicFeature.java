package com.unicorn.rest.server.filter.dynamic;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

import com.unicorn.rest.activities.TokenActivities;
import com.unicorn.rest.activities.UserActivities;
import com.unicorn.rest.server.filter.ActivitiesSecurityFilter;

@Provider
public class ActivitiesSecurityDynamicFeature implements DynamicFeature {
            
    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        Class<?> resourceClass = resourceInfo.getResourceClass();
        String resourceMethod = resourceInfo.getResourceMethod().getName();
        
        /**
         * If the request is to generate/revoke token, or create new user, do not register the security filter
         */
        if (TokenActivities.class.equals(resourceClass) || 
                (UserActivities.class.equals(resourceClass) && resourceMethod.equals("register"))) {
            return;
        }
        context.register(ActivitiesSecurityFilter.class);
    }
}
