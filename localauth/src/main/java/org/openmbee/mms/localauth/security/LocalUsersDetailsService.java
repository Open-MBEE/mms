package org.openmbee.mms.localauth.security;

import org.openmbee.mms.localauth.config.LocalAuthCondition;
import org.openmbee.mms.users.security.DefaultUsersDetailsService;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Conditional(LocalAuthCondition.class)
public class LocalUsersDetailsService extends DefaultUsersDetailsService {}