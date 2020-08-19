/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.am.scenario.tests.rest.api.creation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.ClientAuthenticator;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIMURLBean;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.DCRParamRequest;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.base.MultitenantConstants;

import javax.xml.xpath.XPathExpressionException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DataPopulationTestCase extends ScenarioTestBase {
    private static final Log log = LogFactory.getLog(APIRequest.class);

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String TENANT_ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "micheal";
    private static final String API_CREATOR_PUBLISHER_PW = "Micheal#123";
    private static final String API_SUBSCRIBER_USERNAME = "andrew";
    private static final String API_SUBSCRIBER_PW = "Andrew#123";
    private String tierCollection = "Unlimited";
    private String tag = "APICreationTag";
    private String description = "This is a API creation description";
    private boolean default_version_checked = true;
    private final String APPLICATION_DESCRIPTION = "ApplicationDescription";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private static final String resourcePathLocation = System.getenv("PROJECT_LOCATION")
            + "/product-scenarios/scenarios-common/src/main/resources";
    private static List<String> subscriptionAvailableTenants = new ArrayList<>();

    // All tests in this class will run with a super tenant API creator and a tenant API creator.
    @Factory(dataProvider = "userModeDataProvider")
    public DataPopulationTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws XPathExpressionException {

        System.setProperty("framework.resource.location", resourcePathLocation + "/");
        //create store server instance based on configuration given at automation.xml
        storeContext =
                new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                        APIMIntegrationConstants.AM_STORE_INSTANCE, userMode);
        storeUrls = new APIMURLBean(storeContext.getContextUrls());

        //create publisher server instance based on configuration given at automation.xml
        publisherContext =
                new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                        APIMIntegrationConstants.AM_PUBLISHER_INSTANCE, userMode);
        publisherUrls = new APIMURLBean(publisherContext.getContextUrls());

        //create gateway server instance based on configuration given at automation.xml
        gatewayContextMgt =
                new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                        APIMIntegrationConstants.AM_GATEWAY_MGT_INSTANCE, userMode);
        gatewayUrlsMgt = new APIMURLBean(gatewayContextMgt.getContextUrls());

        gatewayContextWrk =
                new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                        APIMIntegrationConstants.AM_GATEWAY_WRK_INSTANCE, userMode);
        gatewayUrlsWrk = new APIMURLBean(gatewayContextWrk.getContextUrls());

        keyManagerContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE, userMode);
        keyMangerUrl = new APIMURLBean(keyManagerContext.getContextUrls());

        backEndServer = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.BACKEND_SERVER_INSTANCE, userMode);
        backEndServerUrl = new APIMURLBean(backEndServer.getContextUrls());
        setup();
        for (int i = 1; i <= 100; i++) {
            subscriptionAvailableTenants.add(ScenarioTestConstants.TENANT_WSO2);
        }
        subscriptionAvailableTenants.add(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        System.setProperty("javax.net.ssl.trustStore", resourcePathLocation + "/keystores/client-truststore.jks");
    }

    @Test(description = "populateData")
    public void populateData() throws Exception {
        String dcrURL = gatewayUrlsMgt.getWebAppURLHttps() + "client-registration/v0.16/register";
        String apiId;
        String applicationID;
        String subscriptionId = null;
        String accessToken = null;
        for (int i = 1; i <= 100; i++) {
            String tenantDomain = i + ScenarioTestConstants.TENANT_WSO2;
            String tenantAdminUsername = ADMIN_USERNAME + "@" + tenantDomain;
            String publisherUsername = i + API_CREATOR_PUBLISHER_USERNAME;
            String devPortalUsername = i + API_SUBSCRIBER_USERNAME;

            //Add and activate tenant
            addTenantAndActivate(tenantDomain, ADMIN_USERNAME, ADMIN_PW);
            if (isActivated(tenantDomain)) {
                //Add publisher user
                createUserWithPublisherAndCreatorRole(publisherUsername, API_CREATOR_PUBLISHER_PW, tenantAdminUsername,
                        TENANT_ADMIN_PW);

                //DCR call for publisher app.
                DCRParamRequest publisherParamRequest = new DCRParamRequest(RestAPIPublisherImpl.appName,
                        RestAPIPublisherImpl.callBackURL, RestAPIPublisherImpl.tokenScope, RestAPIPublisherImpl.appOwner,
                        RestAPIPublisherImpl.grantType, dcrURL, publisherUsername, API_CREATOR_PUBLISHER_PW, tenantDomain);
                ClientAuthenticator.makeDCRRequest(publisherParamRequest);
                RestAPIPublisherImpl restAPIPublisherNew = new RestAPIPublisherImpl(publisherUsername,
                        API_CREATOR_PUBLISHER_PW, tenantDomain, baseUrl);

                // Wait unlit throttle policies get deployed.
                Thread.sleep(10000);
                for (int j = 1; j <= 10; j++) {
                    apiId = createAPI("SampleAPI" + "_" + i + "_" + j, "/customers" + "_" + i + "_" + j, "/", "1.0.0",
                            publisherUsername + "@" + tenantDomain, restAPIPublisherNew);
                    publishAPI(apiId, restAPIPublisherNew);
                    log.info("API added successfully ID: " + apiId);

                    for (int k = 1; k <= 10; k++) {
                        //Add devPortal user
                        createUserWithSubscriberRole(devPortalUsername + i + "_" + j + "_" + k, API_SUBSCRIBER_PW, tenantAdminUsername,
                                TENANT_ADMIN_PW);
                        //DCR call for dev portal app.
                        DCRParamRequest devPortalParamRequest = new DCRParamRequest(RestAPIStoreImpl.appName,
                                RestAPIStoreImpl.callBackURL, RestAPIStoreImpl.tokenScope, RestAPIStoreImpl.appOwner,
                                RestAPIStoreImpl.grantType, dcrURL, devPortalUsername + i + "_" + j + "_" + k, API_SUBSCRIBER_PW, tenantDomain);
                        ClientAuthenticator.makeDCRRequest(devPortalParamRequest);
                        RestAPIStoreImpl restAPIStoreNew = new RestAPIStoreImpl(devPortalUsername + i + "_" + j + "_" + k, API_SUBSCRIBER_PW,
                                tenantDomain, baseUrl);
                        applicationID = createApplication("SampleApplication" + "_" + i + "_" + j + k, restAPIStoreNew);

                        if (restAPIStoreNew.isAvailableInDevPortal(apiId, tenantDomain)) {
                            subscriptionId = createSubscription(apiId, applicationID, restAPIStoreNew);
                            accessToken = generateKeys(applicationID, restAPIStoreNew);
                        }
                        log.info("APPLICATION created successfully ID: " + applicationID);
                        log.info("SUBSCRIPTION created successfully ID: " + subscriptionId);
                        log.info("ACCESS generated successfully TOKEN: " + accessToken);
                    }
                }
                log.info("Artifacts deployed for tenant: " + tenantDomain);
            }
            System.gc();
        }
        log.info("All the artifacts deployed successfully");
    }

    private String createAPI(String apiName, String apiContext, String apiResource, String apiVersion,
                             String apiProviderName, RestAPIPublisherImpl restAPIPublisher)
            throws ApiException, ParseException, MalformedURLException {

        List<APIOperationsDTO> apiOperationsDTOs = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setTarget(apiResource);
        apiOperationsDTOs.add(apiOperationsDTO);

        List<String> tags = new ArrayList<>();
        tags.add(tag);

        List<String> tiersCollectionList = new ArrayList<>();
        tiersCollectionList.add(tierCollection);

        List<String> transports = new ArrayList<>();
        transports.add("http");
        transports.add("https");

        APIDTO apiCreationDTO = new APIDTO();
        apiCreationDTO.setName(apiName);
        apiCreationDTO.setContext(apiContext);
        apiCreationDTO.setVersion(apiVersion);
        apiCreationDTO.setProvider(apiProviderName);
        apiCreationDTO.setVisibility(APIDTO.VisibilityEnum.PUBLIC);
        apiCreationDTO.setOperations(apiOperationsDTOs);
        apiCreationDTO.setDescription(description);
        apiCreationDTO.setTags(tags);
        apiCreationDTO.policies(tiersCollectionList);
//        apiCreationDTO.setCacheTimeout(Integer.parseInt(cacheTimeout));
//        apiCreationDTO.setResponseCachingEnabled(false);
//        apiCreationDTO.setEndpointSecurity(securityDTO);
//        apiCreationDTO.setBusinessInformation(businessDTO);
        apiCreationDTO.setSubscriptionAvailableTenants(subscriptionAvailableTenants);
        apiCreationDTO.setIsDefaultVersion(default_version_checked);
        apiCreationDTO.setTransport(transports);

        String endpointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;

        URL endpoint = new URL(endpointUrl);
        JSONParser parser = new JSONParser();
        String endPointString = "{\n" +
                "  \"production_endpoints\": {\n" +
                "    \"template_not_supported\": false,\n" +
                "    \"config\": null,\n" +
                "    \"url\": \"" + endpointUrl + "\"\n" +
                "  \"legacy-encoding\": \"" + endpoint + "\"\n" +
                "  },\n" +
                "  \"sandbox_endpoints\": {\n" +
                "    \"url\": \"" + endpointUrl + "\",\n" +
                "    \"config\": null,\n" +
                "    \"template_not_supported\": false\n" +
                "  \"legacy-encoding\": \"" + endpoint + "\"\n" +
                "  },\n" +
                "  \"endpoint_type\": \"http\"\n" +
                "}";

        Object jsonObject = parser.parse(endPointString);
        apiCreationDTO.setEndpointConfig(jsonObject);

        //Design API with name,context,version,visibility,apiResource and with all optional values
        APIDTO apidto = restAPIPublisher.addAPI(apiCreationDTO, apiVersion);
        return apidto.getId();
    }

    private void publishAPI(String apiId, RestAPIPublisherImpl restAPIPublisher) throws ApiException {
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);
    }

    private String createApplication(String applicationName, RestAPIStoreImpl restAPIStore) {
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName,
                APPLICATION_DESCRIPTION, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        return applicationResponse.getData();
    }

    private String createSubscription(String apiId, String applicationId, RestAPIStoreImpl restAPIStore) {
        HttpResponse subscription = restAPIStore.createSubscription(apiId, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);
        return subscription.getData();

    }

    private String generateKeys(String applicationId, RestAPIStoreImpl restAPIStore)
            throws org.wso2.am.integration.clients.store.api.ApiException {
        ArrayList grantTypes = new ArrayList();
        grantTypes.add("client_credentials");
        grantTypes.add("password");
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "3600"
                , null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);

        return applicationKeyDTO.getToken().getAccessToken();
    }

    // This method runs prior to the @BeforeClass method.
    // Create users and tenants needed or the tests in here. Try to reuse the TENANT_WSO2 as much as possible to avoid the number of tenants growing.
    @DataProvider
    public static Object[][] userModeDataProvider() throws Exception {
        setup();
        // return the relevant parameters for each test run
        // 1) Super tenant API creator
        // 2) Tenant API creator
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_USER}
        };
    }
}