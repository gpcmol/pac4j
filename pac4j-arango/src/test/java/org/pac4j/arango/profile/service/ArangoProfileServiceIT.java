package org.pac4j.arango.profile.service;

import com.arangodb.ArangoDB;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.arango.profile.ArangoProfile;
import org.pac4j.arango.test.tools.ArangoServer;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.exception.AccountNotFoundException;
import org.pac4j.core.exception.BadCredentialsException;
import org.pac4j.core.exception.MultipleAccountsFoundException;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.service.AbstractProfileService;
import org.pac4j.core.util.TestsConstants;
import org.pac4j.core.util.TestsHelper;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests the {@link ArangoProfileService}.
 *
 * @author gpcmol
 * @since 1.8.0
 */
public final class ArangoProfileServiceIT implements TestsConstants {

    private static final int PORT = 37017;
    private static final String ARANGO_ID = "arangoId";
    private static final String ARANGO_LINKEDID = "arangoLinkedId";
    private static final String ARANGO_LINKEDID2 = "arangoLinkedId2";
    private static final String ARANGO_USER = "arangoUser";
    private static final String ARANGO_PASS = "arangoPass";
    private static final String ARANGO_PASS2 = "arangoPass2";

    private final ArangoServer arangoServer = new ArangoServer();

    @Before
    public void setUp() {
        arangoServer.start();
    }

    @After
    public void tearDown() {
        arangoServer.stop();
    }

    private ArangoDB getClient() {
        return arangoServer.getClient();
    }

    @Test
    public void testNullPasswordEncoder() {
        final ArangoProfileService authenticator = new ArangoProfileService(getClient(), FIRSTNAME);
        authenticator.setPasswordEncoder(null);
        TestsHelper.expectException(() -> authenticator.init(), TechnicalException.class, "passwordEncoder cannot be null");
    }

    @Test
    public void testNullArangoClient() {
        final ArangoProfileService authenticator = new ArangoProfileService(null, FIRSTNAME, ArangoServer.PASSWORD_ENCODER);
        TestsHelper.expectException(() -> authenticator.init(), TechnicalException.class, "arangoClient cannot be null");
    }

    @Test
    public void testNullDatabase() {
        final ArangoProfileService authenticator = new ArangoProfileService(getClient(), FIRSTNAME, ArangoServer.PASSWORD_ENCODER);
        authenticator.setUsersDatabase(null);
        TestsHelper.expectException(() -> authenticator.init(), TechnicalException.class, "usersDatabase cannot be blank");
    }

    @Test
    public void testNullCollection() {
        final ArangoProfileService authenticator = new ArangoProfileService(getClient(), FIRSTNAME, ArangoServer.PASSWORD_ENCODER);
        authenticator.setUsersCollection(null);
        TestsHelper.expectException(() -> authenticator.init(), TechnicalException.class, "usersCollection cannot be blank");
    }

    @Test
    public void testNullUsername() {
        final ArangoProfileService authenticator = new ArangoProfileService(getClient(), FIRSTNAME, ArangoServer.PASSWORD_ENCODER);
        authenticator.setUsernameAttribute(null);
        TestsHelper.expectException(() -> authenticator.init(), TechnicalException.class, "usernameAttribute cannot be blank");
    }

    @Test
    public void testNullPassword() {
        final ArangoProfileService authenticator = new ArangoProfileService(getClient(), FIRSTNAME, ArangoServer.PASSWORD_ENCODER);
        authenticator.setPasswordAttribute(null);
        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(GOOD_USERNAME, PASSWORD);
        TestsHelper.expectException(() -> authenticator.validate(credentials, null), TechnicalException.class,
            "passwordAttribute cannot be blank");
    }

    private UsernamePasswordCredentials login(final String username, final String password, final String attribute) {
        final ArangoProfileService authenticator = new ArangoProfileService(getClient(), attribute);
        authenticator.setPasswordEncoder(ArangoServer.PASSWORD_ENCODER);
        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
        authenticator.validate(credentials, null);

        return credentials;
    }

    @Test
    public void testGoodUsernameAttribute() {
        final UsernamePasswordCredentials credentials =  login(GOOD_USERNAME, PASSWORD, FIRSTNAME);

        final CommonProfile profile = credentials.getUserProfile();
        assertNotNull(profile);
        assertTrue(profile instanceof ArangoProfile);
        final ArangoProfile dbProfile = (ArangoProfile) profile;
        assertEquals(GOOD_USERNAME, dbProfile.getId());
        assertEquals(FIRSTNAME_VALUE, dbProfile.getAttribute(FIRSTNAME));
    }

    @Test
    public void testGoodUsernameNoAttribute() {
        final UsernamePasswordCredentials credentials = login(GOOD_USERNAME, PASSWORD, "");

        final CommonProfile profile = credentials.getUserProfile();
        assertNotNull(profile);
        assertTrue(profile instanceof ArangoProfile);
        final ArangoProfile dbProfile = (ArangoProfile) profile;
        assertEquals(GOOD_USERNAME, dbProfile.getId());
        assertNull(dbProfile.getAttribute(FIRSTNAME));
    }

    @Test
    public void testMultipleUsername() {
        TestsHelper.expectException(() -> login(MULTIPLE_USERNAME, PASSWORD, ""), MultipleAccountsFoundException.class,
            "Too many accounts found for: misagh");
    }

    @Test
    public void testBadUsername() {
        TestsHelper.expectException(() -> login(BAD_USERNAME, PASSWORD, ""), AccountNotFoundException.class,
            "No account found for: michael");
    }

    @Test
    public void testBadPassword() {
        TestsHelper.expectException(() ->login(GOOD_USERNAME, PASSWORD + "bad", ""), BadCredentialsException.class,
            "Bad credentials for: jle");
    }

    @Test
    public void testCreateUpdateFindDelete() {
        final String objectId = new String("ObjectId");
        final ArangoProfile profile = new ArangoProfile();
        profile.setId(ARANGO_ID);
        profile.setLinkedId(ARANGO_LINKEDID);
        profile.addAttribute(USERNAME, ARANGO_USER);
        profile.addAttribute(FIRSTNAME, objectId);
        final ArangoProfileService arangoProfileService = new ArangoProfileService(getClient());
        arangoProfileService.setPasswordEncoder(ArangoServer.PASSWORD_ENCODER);
        // create
        arangoProfileService.create(profile, ARANGO_PASS);
        // check credentials
        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(ARANGO_USER, ARANGO_PASS);
        arangoProfileService.validate(credentials, null);
        final CommonProfile profile1 = credentials.getUserProfile();
        assertNotNull(profile1);
        // check data
        final List<Map<String, Object>> results = getData(arangoProfileService, ARANGO_ID);
        assertEquals(1, results.size());
        final Map<String, Object> result = results.get(0);
        assertEquals(5, result.size());
        assertEquals(ARANGO_ID, result.get(ID));
        assertEquals(ARANGO_LINKEDID, result.get(AbstractProfileService.LINKEDID));
        assertNotNull(result.get(AbstractProfileService.SERIALIZED_PROFILE));
        assertTrue(ArangoServer.PASSWORD_ENCODER.matches(ARANGO_PASS, (String) result.get(PASSWORD)));
        assertEquals(ARANGO_USER, result.get(USERNAME));
        // findById
        final ArangoProfile profile2 = arangoProfileService.findByLinkedId(ARANGO_LINKEDID);
        assertEquals(ARANGO_ID, profile2.getId());
        assertEquals(ARANGO_LINKEDID, profile2.getLinkedId());
        assertEquals(ARANGO_USER, profile2.getUsername());
        assertEquals(objectId, profile2.getAttribute(FIRSTNAME));
        assertEquals(2, profile2.getAttributes().size());
        // update
        profile.setLinkedId(ARANGO_LINKEDID2);
        arangoProfileService.update(profile, ARANGO_PASS2);
        final List<Map<String, Object>> results2 = getData(arangoProfileService, ARANGO_ID);
        assertEquals(1, results2.size());
        final Map<String, Object> result2 = results2.get(0);
        assertEquals(5, result2.size());
        assertEquals(ARANGO_ID, result2.get(ID));
        assertEquals(ARANGO_LINKEDID2, result2.get(AbstractProfileService.LINKEDID));
        assertNotNull(result2.get(AbstractProfileService.SERIALIZED_PROFILE));
        assertTrue(ArangoServer.PASSWORD_ENCODER.matches(ARANGO_PASS2, (String) result2.get(PASSWORD)));
        assertEquals(ARANGO_USER, result2.get(USERNAME));
        // remove
        arangoProfileService.remove(profile);
        final List<Map<String, Object>> results3 = getData(arangoProfileService, ARANGO_ID);
        assertEquals(0, results3.size());
    }

    private List<Map<String, Object>> getData(final ArangoProfileService service, final String id) {
        return service.read(null, _KEY, id);
    }
}
