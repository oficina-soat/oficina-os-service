package br.com.oficina.os.core.entities.cliente;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CpfTest {
    @Test
    void deveCriarComSucesso() {
        Assertions.assertDoesNotThrow(() -> new Cpf("84191404067"));
        Assertions.assertDoesNotThrow(() -> new Cpf("84191404067"));
        Assertions.assertDoesNotThrow(() -> new Cpf("841.914.04067"));
        Assertions.assertDoesNotThrow(() -> new Cpf("841.914.040-67"));
        Assertions.assertDoesNotThrow(() -> new Cpf("841.914040-67"));
        Assertions.assertDoesNotThrow(() -> new Cpf("841914.04067"));
        Assertions.assertDoesNotThrow(() -> new Cpf("841914.040-67"));
        Assertions.assertDoesNotThrow(() -> new Cpf("841914040-67"));
    }

    @Test
    void deveLancarQuandoInvalido() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Cpf("8419140406"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Cpf("84191404068"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Cpf("841914040671"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Cpf("11111111111"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Cpf(""));
        Assertions.assertThrows(NullPointerException.class, () -> new Cpf(null));
    }
}