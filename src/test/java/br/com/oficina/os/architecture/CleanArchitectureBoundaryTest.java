package br.com.oficina.os.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CleanArchitectureBoundaryTest {
    private static final Path MAIN = Path.of("src/main/java/br/com/oficina/os");

    @Test
    void coreNaoDependeDeFrameworkInterfacesOuRuntime() throws IOException {
        var violations = javaFiles(MAIN.resolve("core")).stream()
                .filter(path -> containsAny(path, List.of(
                        "import br.com.oficina.os.framework.",
                        "import br.com.oficina.os.interfaces.",
                        "import jakarta.",
                        "import javax.",
                        "import io.quarkus.",
                        "import org.jboss.")))
                .toList();

        assertTrue(violations.isEmpty(), "Core deve depender apenas de entidades, portas e Java: " + violations);
    }

    @Test
    void camadaDeInterfaceNaoDependeDeAdaptersDeFramework() throws IOException {
        var violations = javaFiles(MAIN.resolve("interfaces")).stream()
                .filter(path -> containsAny(path, List.of("import br.com.oficina.os.framework.")))
                .toList();

        assertTrue(violations.isEmpty(), "Interfaces/controllers/presenters nao devem importar framework: " + violations);
    }

    @Test
    void portasEUseCasesDevemTerClassesConcretas() throws IOException {
        assertFalse(javaFiles(MAIN.resolve("core/interfaces")).isEmpty(), "core/interfaces nao pode ficar vazio");
        assertFalse(javaFiles(MAIN.resolve("core/usecases")).isEmpty(), "core/usecases nao pode ficar vazio");
    }

    @Test
    void atendimentoSeedStoreDeveSerApenasFacadeDeSelecaoDeAdapter() throws IOException {
        var facade = MAIN.resolve("framework/db/AtendimentoSeedStore.java");
        var lines = Files.readAllLines(facade).size();

        assertTrue(lines <= 230, "AtendimentoSeedStore deve permanecer como facade pequeno; linhas atuais: " + lines);
    }

    private static List<Path> javaFiles(Path root) throws IOException {
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }
    }

    private static boolean containsAny(Path path, List<String> fragments) {
        try {
            var content = Files.readString(path);
            return fragments.stream().anyMatch(content::contains);
        } catch (IOException exception) {
            throw new IllegalStateException("Falha ao ler " + path, exception);
        }
    }
}
