package br.com.oficina.os.framework.web;

import br.com.oficina.os.framework.messaging.AwsDomainMessagingClient;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ConfigUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.sql.DataSource;
import org.eclipse.microprofile.config.Config;

@ApplicationScoped
public class RuntimeStartupValidator {
    private static final String DEPLOYMENT_ENVIRONMENT = "oficina.observability.deployment-environment";
    private static final String PERSISTENCE_KIND = "oficina.persistence.kind";
    private static final String MESSAGING_ENDPOINT = "oficina.messaging.endpoint-override";
    private static final String AWS_ACCESS_KEY_ID = "oficina.messaging.aws-access-key-id";
    private static final String AWS_SECRET_ACCESS_KEY = "oficina.messaging.aws-secret-access-key";
    private static final String AWS_SESSION_TOKEN = "oficina.messaging.aws-session-token";
    private static final String CANONICAL_AUDIENCE = "oficina-os-service";
    private static final List<String> REQUIRED_PROTECTED_PROPERTIES = List.of(
            "quarkus.datasource.username",
            "quarkus.datasource.password",
            "quarkus.datasource.jdbc.url",
            "quarkus.datasource.reactive.url",
            "AWS_REGION",
            "mp.jwt.verify.issuer",
            "mp.jwt.verify.audiences",
            "mp.jwt.verify.publickey.location");
    private static final List<String> REQUIRED_MESSAGING_FLAGS = List.of(
            "oficina.messaging.enabled",
            "oficina.messaging.publisher.enabled",
            "oficina.messaging.consumer.enabled",
            "oficina.messaging.worker.enabled");

    private final Config config;
    private final Instance<DataSource> dataSources;
    private final AwsDomainMessagingClient messagingClient;

    @Inject
    RuntimeStartupValidator(Config config, Instance<DataSource> dataSources, AwsDomainMessagingClient messagingClient) {
        this.config = config;
        this.dataSources = dataSources;
        this.messagingClient = messagingClient;
    }

    void validarNoStartup(@Observes StartupEvent ignored) {
        var profiles = ConfigUtils.getProfiles();
        var values = configurationValues(config);
        validarConfiguracao(profiles, values);
        if (!runtimeProtegido(profiles, values.get(DEPLOYMENT_ENVIRONMENT))) {
            return;
        }
        validarPostgres(dataSources.get());
        try {
            messagingClient.validarDependencias();
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Dependências SNS/SQS obrigatórias não estão acessíveis no startup do oficina-os-service.",
                    exception);
        }
    }

    static void validarConfiguracao(Collection<String> profiles, Map<String, String> values) {
        var violations = new ArrayList<String>();
        var persistenceKind = normalized(values.get(PERSISTENCE_KIND));
        if (!persistenceKind.equals("postgresql") && !persistenceKind.equals("memory")) {
            violations.add(PERSISTENCE_KIND + " deve ser postgresql ou memory");
        }
        validarParDeCredenciaisAws(values, violations);

        if (runtimeProtegido(profiles, values.get(DEPLOYMENT_ENVIRONMENT))) {
            if (!persistenceKind.equals("postgresql")) {
                violations.add(PERSISTENCE_KIND + " deve ser postgresql em prod/lab");
            }
            REQUIRED_PROTECTED_PROPERTIES.stream()
                    .filter(property -> missing(values.get(property)))
                    .map(property -> property + " é obrigatória em prod/lab")
                    .forEach(violations::add);
            REQUIRED_MESSAGING_FLAGS.stream()
                    .filter(property -> !Boolean.parseBoolean(normalized(values.get(property))))
                    .map(property -> property + " deve ser true em prod/lab")
                    .forEach(violations::add);
            if (!normalized(values.get(MESSAGING_ENDPOINT)).isEmpty()) {
                violations.add(MESSAGING_ENDPOINT + " não é permitido em prod/lab");
            }
            if (!CANONICAL_AUDIENCE.equals(normalized(values.get("mp.jwt.verify.audiences")))) {
                violations.add("mp.jwt.verify.audiences deve ser " + CANONICAL_AUDIENCE + " em prod/lab");
            }
        }

        if (!violations.isEmpty()) {
            throw new IllegalStateException("Configuração de runtime inválida: " + String.join("; ", violations));
        }
    }

    static boolean runtimeProtegido(Collection<String> profiles, String deploymentEnvironment) {
        return profiles.stream().anyMatch(profile -> "prod".equalsIgnoreCase(profile) || "lab".equalsIgnoreCase(profile))
                || "lab".equalsIgnoreCase(normalized(deploymentEnvironment));
    }

    static void validarPostgres(DataSource dataSource) {
        try (var connection = dataSource.getConnection()) {
            if (!connection.isValid(5)) {
                throw new IllegalStateException("PostgreSQL obrigatório não respondeu à validação de conexão.");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("PostgreSQL obrigatório não está acessível no startup.", exception);
        }
    }

    private static Map<String, String> configurationValues(Config config) {
        var values = new LinkedHashMap<String, String>();
        values.put(DEPLOYMENT_ENVIRONMENT, value(config, DEPLOYMENT_ENVIRONMENT));
        values.put(PERSISTENCE_KIND, value(config, PERSISTENCE_KIND));
        values.put(MESSAGING_ENDPOINT, value(config, MESSAGING_ENDPOINT));
        values.put(AWS_ACCESS_KEY_ID, value(config, AWS_ACCESS_KEY_ID));
        values.put(AWS_SECRET_ACCESS_KEY, value(config, AWS_SECRET_ACCESS_KEY));
        values.put(AWS_SESSION_TOKEN, value(config, AWS_SESSION_TOKEN));
        REQUIRED_PROTECTED_PROPERTIES.forEach(property -> values.put(property, value(config, property)));
        REQUIRED_MESSAGING_FLAGS.forEach(property -> values.put(property, value(config, property)));
        return Map.copyOf(values);
    }

    private static String value(Config config, String property) {
        return config.getOptionalValue(property, String.class).orElse("");
    }

    private static void validarParDeCredenciaisAws(Map<String, String> values, List<String> violations) {
        var accessKeyConfigured = !normalized(values.get(AWS_ACCESS_KEY_ID)).isEmpty();
        var secretKeyConfigured = !normalized(values.get(AWS_SECRET_ACCESS_KEY)).isEmpty();
        var sessionTokenConfigured = !normalized(values.get(AWS_SESSION_TOKEN)).isEmpty();
        if (accessKeyConfigured != secretKeyConfigured
                || (sessionTokenConfigured && (!accessKeyConfigured || !secretKeyConfigured))) {
            violations.add("credenciais AWS explícitas estão incompletas");
        }
    }

    private static boolean missing(String value) {
        var normalized = normalized(value);
        return normalized.isEmpty() || normalized.toUpperCase(Locale.ROOT).contains("PLACEHOLDER");
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim();
    }
}
