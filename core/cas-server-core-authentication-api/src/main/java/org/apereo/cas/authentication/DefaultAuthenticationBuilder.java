package org.apereo.cas.authentication;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.util.CollectionUtils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Constructs immutable {@link Authentication} objects using the builder pattern.
 *
 * @author Marvin S. Addison
 * @since 4.0.0
 */
@Slf4j
@Getter
public class DefaultAuthenticationBuilder implements AuthenticationBuilder {

    private static final long serialVersionUID = -8504842011648432398L;

    /**
     * Authenticated principal.
     */
    private Principal principal;

    /**
     * Credential metadata.
     */
    private final List<CredentialMetaData> credentials = new ArrayList<>();

    /**
     * Authentication metadata attributes.
     */
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    /**
     * Map of handler names to authentication successes.
     */
    private final Map<String, AuthenticationHandlerExecutionResult> successes = new LinkedHashMap<>();

    /**
     * Map of handler names to authentication failures.
     */
    private final Map<String, Throwable> failures = new LinkedHashMap<>();

    /**
     * Authentication date.
     */
    private ZonedDateTime authenticationDate;

    /**
     * Creates a new instance using the current date for the authentication date.
     */
    public DefaultAuthenticationBuilder() {
        this.authenticationDate = ZonedDateTime.now();
    }

    /**
     * Creates a new instance using the current date for the authentication date and the given
     * principal for the authenticated principal.
     *
     * @param p Authenticated principal.
     */
    public DefaultAuthenticationBuilder(final Principal p) {
        this();
        this.principal = p;
    }

    /**
     * Sets the authentication date and returns this instance.
     *
     * @param d Authentication date.
     * @return This builder instance.
     */
    @Override
    public AuthenticationBuilder setAuthenticationDate(final ZonedDateTime d) {
        this.authenticationDate = d;
        return this;
    }

    @Override
    public AuthenticationBuilder addCredentials(final List<CredentialMetaData> credentials) {
        this.credentials.addAll(credentials);
        return this;
    }

    /**
     * Sets the principal returns this instance.
     *
     * @param p Authenticated principal.
     * @return This builder instance.
     */
    @Override
    public AuthenticationBuilder setPrincipal(final Principal p) {
        this.principal = p;
        return this;
    }

    /**
     * Sets the list of metadata about credentials presented for authentication.
     *
     * @param credentials Non-null list of credential metadata.
     * @return This builder instance.
     */
    public AuthenticationBuilder setCredentials(@NonNull final List<CredentialMetaData> credentials) {
        this.credentials.clear();
        this.credentials.addAll(credentials);
        return this;
    }

    /**
     * Adds metadata about a credential presented for authentication.
     *
     * @param credential Credential metadata.
     * @return This builder instance.
     */
    @Override
    public AuthenticationBuilder addCredential(final CredentialMetaData credential) {
        this.credentials.add(credential);
        return this;
    }

    /**
     * Sets the authentication metadata attributes.
     *
     * @param attributes Non-null map of authentication metadata attributes.
     * @return This builder instance.
     */
    @Override
    public AuthenticationBuilder setAttributes(final Map<String, Object> attributes) {
        this.attributes.clear();
        this.attributes.putAll(attributes);
        return this;
    }

    @Override
    public AuthenticationBuilder mergeAttribute(final String key, final Object value) {
        final Object currentValue = this.attributes.get(key);
        if (currentValue == null) {
            return addAttribute(key, value);
        }
        final Collection collection = CollectionUtils.toCollection(currentValue);
        collection.addAll(CollectionUtils.toCollection(value));
        return addAttribute(key, collection);
    }

    @Override
    public boolean hasAttribute(final String name, final Predicate<Object> predicate) {
        if (this.attributes.containsKey(name)) {
            final Object value = this.attributes.get(name);
            final Collection valueCol = CollectionUtils.toCollection(value);
            return valueCol.stream().anyMatch(predicate);
        }
        return false;
    }

    /**
     * Adds an authentication metadata attribute key-value pair.
     *
     * @param key   Authentication attribute key.
     * @param value Authentication attribute value.
     * @return This builder instance.
     */
    @Override
    public AuthenticationBuilder addAttribute(final String key, final Object value) {
        this.attributes.put(key, value);
        return this;
    }

    /**
     * Sets the authentication handler success map.
     *
     * @param successes Non-null map of handler names to successful handler authentication results.
     * @return This builder instance.
     */
    @Override
    public AuthenticationBuilder setSuccesses(@NonNull final Map<String, AuthenticationHandlerExecutionResult> successes) {
        this.successes.clear();
        return addSuccesses(successes);
    }

    @Override
    public AuthenticationBuilder addSuccesses(final Map<String, AuthenticationHandlerExecutionResult> successes) {
        successes.entrySet().forEach(entry -> addSuccess(entry.getKey(), entry.getValue()));
        return this;
    }

    /**
     * Adds an authentication success to the map of handler names to successful authentication handler results.
     *
     * @param key   Authentication handler name.
     * @param value Successful authentication handler result produced by handler of given name.
     * @return This builder instance.
     */
    @Override
    public AuthenticationBuilder addSuccess(final String key, final AuthenticationHandlerExecutionResult value) {
        LOGGER.debug("Recording authentication handler result success under key [{}]", key);
        if (this.successes.containsKey(key)) {
            LOGGER.debug("Key mapped to authentication handler result [{}] is already recorded in the list of successful attempts. Overriding...", key);
        }
        this.successes.put(key, value);
        return this;
    }

    /**
     * Sets the authentication handler failure map.
     *
     * @param failures Non-null map of handler name to authentication failures.
     * @return This builder instance.
     */
    @Override
    public AuthenticationBuilder setFailures(@NonNull final Map<String, Throwable> failures) {
        this.failures.clear();
        return addFailures(failures);
    }

    @Override
    public AuthenticationBuilder addFailures(final Map<String, Throwable> failures) {
        failures.entrySet().forEach(entry -> addFailure(entry.getKey(), entry.getValue()));
        return this;
    }

    /**
     * Adds an authentication failure to the map of handler names to the authentication handler failures.
     *
     * @param key   Authentication handler name.
     * @param value Exception raised on handler failure to authenticate credential.
     * @return This builder instance.
     */
    @Override
    public AuthenticationBuilder addFailure(final String key, final Throwable value) {
        LOGGER.debug("Recording authentication handler failure under key [{}]", key);
        if (this.successes.containsKey(key)) {
            final String newKey = key + System.currentTimeMillis();
            LOGGER.debug("Key mapped to authentication handler failure [{}] is recorded in the list of failed attempts. Overriding with [{}]", key, newKey);
            this.failures.put(newKey, value);
        } else {
            this.failures.put(key, value);
        }
        return this;
    }

    /**
     * Creates an immutable authentication instance from builder data.
     *
     * @return Immutable authentication.
     */
    @Override
    public Authentication build() {
        return new DefaultAuthentication(this.authenticationDate, this.credentials, this.principal, this.attributes, this.successes, this.failures);
    }

    /**
     * Creates a new builder initialized with data from the given authentication source.
     *
     * @param source Authentication source.
     * @return New builder instance initialized with all fields in the given authentication source.
     */
    public static AuthenticationBuilder newInstance(final Authentication source) {
        final DefaultAuthenticationBuilder builder = new DefaultAuthenticationBuilder(source.getPrincipal());
        builder.setAuthenticationDate(source.getAuthenticationDate());
        builder.setCredentials(source.getCredentials());
        builder.setSuccesses(source.getSuccesses());
        builder.setFailures(source.getFailures());
        builder.setAttributes(source.getAttributes());
        return builder;
    }

    /**
     * Creates a new builder.
     *
     * @return New builder instance
     */
    public static AuthenticationBuilder newInstance() {
        return new DefaultAuthenticationBuilder();
    }
}
