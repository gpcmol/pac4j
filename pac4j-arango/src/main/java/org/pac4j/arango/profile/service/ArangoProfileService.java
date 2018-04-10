package org.pac4j.arango.profile.service;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.BaseDocument;
import org.pac4j.arango.profile.ArangoProfile;
import org.pac4j.core.credentials.password.PasswordEncoder;
import org.pac4j.core.profile.definition.CommonProfileDefinition;
import org.pac4j.core.profile.service.AbstractProfileService;
import org.pac4j.core.util.CommonHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The ArangoDB profile service (which supersedes the Arango authenticator).
 *
 * @author gpcmol
 * @since 2.0.0
 */
public class ArangoProfileService extends AbstractProfileService<ArangoProfile> {

    private ArangoDB arangoClient;

    private String usersDatabase = "users";
    private String usersCollection = "users";

    public ArangoProfileService() {}

    public ArangoProfileService(final ArangoDB arangoClient) {
        this.arangoClient = arangoClient;
    }

    public ArangoProfileService(final ArangoDB arangoClient, final String attributes) {
        this.arangoClient = arangoClient;
        setAttributes(attributes);
    }

    public ArangoProfileService(final ArangoDB arangoClient, final String attributes, final PasswordEncoder passwordEncoder) {
        this.arangoClient = arangoClient;
        setAttributes(attributes);
        setPasswordEncoder(passwordEncoder);
    }

    public ArangoProfileService(final ArangoDB arangoClient, final PasswordEncoder passwordEncoder) {
        this.arangoClient = arangoClient;
        setPasswordEncoder(passwordEncoder);
    }

    @Override
    protected void internalInit() {
        CommonHelper.assertNotNull("passwordEncoder", getPasswordEncoder());
        CommonHelper.assertNotNull("arangoClient", this.arangoClient);
        CommonHelper.assertNotBlank("usersDatabase", this.usersDatabase);
        CommonHelper.assertNotBlank("usersCollection", this.usersCollection);
        defaultProfileDefinition(new CommonProfileDefinition<>(x -> new ArangoProfile()));

        super.internalInit();
    }

    @Override
    protected void insert(final Map<String, Object> attributes) {
        final BaseDocument doc = new BaseDocument();
        for (final Map.Entry<String, Object> entry : attributes.entrySet()) {
            doc.addAttribute(entry.getKey(), entry.getValue());
        }

        logger.debug("Insert doc: {}", doc);
        getCollection().insertDocument(doc);
    }

    @Override
    protected void update(final Map<String, Object> attributes) {
        String id = null;
        final BaseDocument doc = new BaseDocument();
        for (final Map.Entry<String, Object> entry : attributes.entrySet()) {
            final String name = entry.getKey();
            final Object value = entry.getValue();
            if (ID.equals(name)) {
                id = (String) value;
            } else {
                doc.addAttribute(entry.getKey(), entry.getValue());
            }
        }

        CommonHelper.assertNotNull(ID, id);
        logger.debug("Updating id: {} with doc: {}", id, doc);
        getCollection().updateDocument(id, doc);
    }

    @Override
    protected void deleteById(final String id) {

        logger.debug("Delete id: {}", id);
        getCollection().deleteDocument(id);
    }

    @Override
    protected List<Map<String, Object>> read(final List<String> names, final String key, final String value) {

        logger.debug("Reading key / value: {} / {}", key, value);
        final List<Map<String, Object>> listAttributes = new ArrayList<>();

        String queryString = String.format("FOR c IN %s FILTER c.%s == %s RETURN c", usersCollection, key, value);

        try (final ArangoCursor<BaseDocument> cursor = arangoClient.db().query(queryString, null, null, BaseDocument.class)) {
            int i = 0;
            while (cursor.hasNext() && i <= 2) {
                final BaseDocument result = cursor.next();
                final Map<String, Object> newAttributes = new HashMap<>();
                // filter on names
                for (final Map.Entry<String, Object> entry : result.getProperties().entrySet()) {
                    final String name = entry.getKey();
                    if (names == null || names.contains(name)) {
                        newAttributes.put(name, entry.getValue());
                    }
                }
                listAttributes.add(newAttributes);
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.debug("Found: ", listAttributes);

        return listAttributes;
    }

    protected ArangoCollection getCollection() {
        final ArangoDatabase db = arangoClient.db(usersDatabase);
        return db.collection(usersCollection);
    }

    public String getUsersDatabase() {
        return usersDatabase;
    }

    public void setUsersDatabase(final String usersDatabase) {
        this.usersDatabase = usersDatabase;
    }

    public String getUsersCollection() {
        return usersCollection;
    }

    public void setUsersCollection(final String usersCollection) {
        this.usersCollection = usersCollection;
    }

    public ArangoDB getArangoClient() {
        return arangoClient;
    }

    public void setArangoClient(final ArangoDB arangoClient) {
        this.arangoClient = arangoClient;
    }

    @Override
    public String toString() {
        return CommonHelper.toNiceString(this.getClass(), "arangoClient", arangoClient, "usersCollection", usersCollection,
            "passwordEncoder", getPasswordEncoder(), "usersDatabase", usersDatabase, "attributes", getAttributes(),
            "profileDefinition", getProfileDefinition(), "idAttribute", getIdAttribute(),
            "usernameAttribute", getUsernameAttribute(), "passwordAttribute", getPasswordAttribute());
    }
}
