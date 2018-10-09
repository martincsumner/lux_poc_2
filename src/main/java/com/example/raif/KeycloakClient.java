package com.example.raif;

/**
 * Created by martins_rh on 01/10/2018.
 */
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
public class KeycloakClient {

    @Value("${keycloak.serverUrl}")
    private String SERVER_URL;

    @Value("${keycloak.realm}")
    private String REALM;

    @Value("${keycloak.username}")
    private String USERNAME;

    @Value("${keycloak.password}")
    private String PASSWORD;

    @Value("${keycloak.clientId}")
    private String CLIENT_ID;

    protected final Log logger = LogFactory.getLog(getClass());

    @RequestMapping(value="/ping", method= RequestMethod.GET)
    public String ping() {
        return "...pong";
    }

    @RequestMapping(
            value="/createUser",
            method=RequestMethod.POST)
    public int createRHSSOUser(@RequestHeader(required=true) final String username, @RequestHeader(required=true) final String firstname, @RequestHeader(required=true) final String secondname, @RequestHeader(required=true) final String password) {
        logger.info("attempting to save user : " + username);
        return createAccount(username,firstname, secondname, password );

    }

    public int createAccount(final String username, final String firstname, final String secondname, final String password) {

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setFirstName(firstname);
        user.setLastName(secondname);

        //this matters else you will not be able to login again once registered!
        user.setEnabled(true);

        user.singleAttribute("acustomAttribute", "acustomAttribute");
        user.setCredentials(Arrays.asList(credential));
        javax.ws.rs.core.Response response = getInstance().realm(REALM).users().create(user);
        final int status = response.getStatus();
        if (status != HttpStatus.CREATED.value()) {
            return status;
        }
        final String createdId = KeyCloakUtil.getCreatedId(response);


        // Reset password
        CredentialRepresentation newCredential = new CredentialRepresentation();
        UserResource userResource = getInstance().realm(REALM).users().get(createdId);
        newCredential.setType(CredentialRepresentation.PASSWORD);
        newCredential.setValue(password);
        newCredential.setTemporary(false);
        userResource.resetPassword(newCredential);
        return HttpStatus.CREATED.value();
    }


    private Keycloak getInstance() {
        return KeycloakBuilder
                .builder()
                .serverUrl(SERVER_URL)
                .realm(REALM)
                .username(USERNAME)
                .password(PASSWORD)
                .clientId(CLIENT_ID)
                .build();
    }
}
