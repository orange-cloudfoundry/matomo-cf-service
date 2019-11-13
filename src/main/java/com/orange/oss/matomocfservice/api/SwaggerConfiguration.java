/*
 * Copyright 2019 Orange and the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.orange.oss.matomocfservice.api;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.AlternateTypeRule;
import springfox.documentation.schema.WildcardType;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.Tag;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author P. Déchamboux
 *
 */
@Configuration
@EnableSwagger2
public class SwaggerConfiguration {
	private final static Logger LOGGER = LoggerFactory.getLogger(SwaggerConfiguration.class);
	@Value("${matomo-service.contact.name:\"Matomo by Orange Open Source\"}")
	private String contactName;
	@Value("${matomo-service.contact.url:\"https://github.com/orange-cloudfoundry/matomo-cf-service\"}")
	private String contactUrl;
	@Value("${matomo-service.contact.email:\"none\"}")
	private String contactEmail;

	public SwaggerConfiguration(){
		LOGGER.debug("CONFIG::Swagger: Initializing Swagger");
	}

	@Bean
	public Docket platformApi() {
		return new Docket(DocumentationType.SWAGGER_2)
				.select()
                	.apis(RequestHandlerSelectors.basePackage("com.orange.oss.matomocfservice.api"))
                	.paths(PathSelectors.regex("/adminapi/platforms.*"))
					.build()
				.tags(new Tag("Platforms and Matomo Instances Admin", "Manage Platforms and Matomo Instances"))					
				.apiInfo(apiInfo())
	                .forCodeGeneration(true)
	                .alternateTypeRules(
	                		getAlternateTypeRule(Collection.class, WildcardType.class, List.class, WildcardType.class)	                
					)        
	                .directModelSubstitute(XMLGregorianCalendar.class, Date.class)
	                .ignoredParameterTypes(Pageable.class)
	                ;
	    
   }
	   	    
   private AlternateTypeRule getAlternateTypeRule(Type sourceType, Type sourceGenericType, Type targetType, Type targetGenericType) {
	   TypeResolver typeResolver = new TypeResolver();
	   ResolvedType source = typeResolver.resolve(sourceType, sourceGenericType);
	   ResolvedType target = typeResolver.resolve(targetType, targetGenericType);
	   return new AlternateTypeRule(source, target);
   }

   private ApiInfo apiInfo() {
	   LOGGER.info("CONFIG::Swagger: contactName={}, contactUrl={}, contactEmail={}", contactName, contactUrl, contactEmail);
	   return new ApiInfoBuilder()
			   .title("Platforms and Matomo Instances Management API")
	           .contact(new Contact(contactName, contactUrl, contactEmail))
	           .version("0.1")
	           .build();
   }	   
}
