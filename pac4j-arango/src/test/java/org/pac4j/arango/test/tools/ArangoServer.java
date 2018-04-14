package org.pac4j.arango.test.tools;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDB;
import com.arangodb.entity.BaseDocument;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.pac4j.core.credentials.password.PasswordEncoder;
import org.pac4j.core.credentials.password.ShiroPasswordEncoder;
import org.pac4j.core.util.TestsConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Simulates a MongoDB server.
 *
 * @author Jerome Leleu
 * @since 1.8.0
 */
public final class ArangoServer implements TestsConstants {

    public final static PasswordEncoder PASSWORD_ENCODER = new ShiroPasswordEncoder(new DefaultPasswordService());

    public void start() {
        ArangoDB arangoDb = getClient();

        // populate
        if (arangoDb.db("users") == null) {
            arangoDb.createDatabase("users");
        }
        if (arangoDb.db("users").collection("users").exists() == false) {
            arangoDb.db("users").createCollection("users");
        }

        final ArangoCollection collection = arangoDb.db("users").collection("users");
        collection.truncate();

        final String password = PASSWORD_ENCODER.encode(PASSWORD);
        Map<String, Object> properties1 = new HashMap<>();
        properties1.put(USERNAME, GOOD_USERNAME);
        properties1.put(PASSWORD, password);
        properties1.put(FIRSTNAME, FIRSTNAME_VALUE);
        collection.insertDocument(new BaseDocument(properties1));
        Map<String, Object> properties2 = new HashMap<>();
        properties2.put(USERNAME, MULTIPLE_USERNAME);
        properties2.put(PASSWORD, password);
        collection.insertDocument(new BaseDocument(properties2));
        Map<String, Object> properties3 = new HashMap<>();
        properties3.put(USERNAME, MULTIPLE_USERNAME);
        properties3.put(PASSWORD, password);
        collection.insertDocument(new BaseDocument(properties3));
    }

    public ArangoDB getClient() {
        String arango_dbHost = "localhost";
        int arango_dbPort = 8529;
        String arango_rootUser = "root";
        String arango_rootPassword = "root";

        ArangoDB arangoDb = new ArangoDB.Builder().host(arango_dbHost, arango_dbPort).user(arango_rootUser).password(arango_rootPassword).build();
        return arangoDb;
    }

    public void stop() {
        this.cleanup();
    }

    public void cleanup() {
        getClient().db("users").collection("users").drop();
    }
}
