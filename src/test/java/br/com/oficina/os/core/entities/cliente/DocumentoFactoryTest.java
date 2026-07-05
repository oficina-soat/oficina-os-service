package br.com.oficina.os.core.entities.cliente;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.smallrye.common.constraint.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentoFactoryTest {
    @Nested
    @DisplayName("Casos inválidos de entrada")
    class InvalidInput {

        @Test
        @DisplayName("Deve lançar exceção quando input for null")
        void shouldThrowWhenInputIsNull() {
            var ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> DocumentoFactory.from(null)
            );

            assertEquals("Documento vazio", ex.getMessage());
        }

        @Test
        @DisplayName("Deve lançar exceção quando input for vazio")
        void shouldThrowWhenInputIsEmpty() {
            var ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> DocumentoFactory.from("")
            );

            assertEquals("Documento vazio", ex.getMessage());
        }

        @Test
        @DisplayName("Deve lançar exceção quando input for apenas espaços")
        void shouldThrowWhenInputIsBlank() {
            var ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> DocumentoFactory.from("   ")
            );

            assertEquals("Documento vazio", ex.getMessage());
        }

        @Test
        @DisplayName("Deve lançar exceção quando quantidade de dígitos for inválida")
        void shouldThrowWhenInvalidLength() {
            var ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> DocumentoFactory.from("123456789")
            );

            assertEquals("Documento inválido", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Casos válidos")
    class ValidInput {

        @Test
        @DisplayName("Deve criar CPF quando houver 11 dígitos")
        void shouldCreateCpf() {
            Documento documento = DocumentoFactory.from("542.818.670-40");

            assertNotNull(documento);
            assertInstanceOf(Cpf.class, documento);
        }

        @Test
        @DisplayName("Deve criar CPF mesmo sem formatação")
        void shouldCreateCpfWithoutMask() {
            Documento documento = DocumentoFactory.from("54281867040");

            assertInstanceOf(Cpf.class, documento);
        }

        @Test
        @DisplayName("Deve criar CNPJ quando houver 14 dígitos")
        void shouldCreateCnpj() {
            Documento documento = DocumentoFactory.from("04.252.011/0001-10");

            assertNotNull(documento);
            assertInstanceOf(Cnpj.class, documento);
        }

        @Test
        @DisplayName("Deve criar CNPJ mesmo sem formatação")
        void shouldCreateCnpjWithoutMask() {
            Documento documento = DocumentoFactory.from("04252011000110");

            assertInstanceOf(Cnpj.class, documento);
        }
    }
}
