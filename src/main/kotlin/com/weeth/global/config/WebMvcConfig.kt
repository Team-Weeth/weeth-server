package com.weeth.global.config

import com.weeth.global.auth.resolver.CurrentUserArgumentResolver
import com.weeth.global.auth.resolver.CurrentUserRoleArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(CurrentUserArgumentResolver())
        resolvers.add(CurrentUserRoleArgumentResolver())
    }
}
