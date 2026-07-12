package br.com.oficina.os.core.entities.pessoa;

import br.com.oficina.os.core.entities.cliente.Cnpj;
import br.com.oficina.os.core.entities.cliente.Cpf;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PessoaTest {

    @Test
    void deveCriarPessoaFisicaEAtualizarDocumentoParaPessoaJuridica() {
        var pessoaId = UUID.randomUUID();
        var pessoa = new Pessoa(pessoaId, new Cpf("84191404067"), " Maria Silva ");

        assertEquals(pessoaId, pessoa.id());
        assertEquals("84191404067", pessoa.documento().valor());
        assertEquals(TipoPessoa.FISICA, pessoa.tipoPessoa());
        assertEquals("Maria Silva", pessoa.nome());

        pessoa.alteraDocumentoPara(new Cnpj("04252011000110"));
        pessoa.alteraNomePara(" Oficina Central ");

        assertEquals("04252011000110", pessoa.documento().valor());
        assertEquals(TipoPessoa.JURIDICA, pessoa.tipoPessoa());
        assertEquals("Oficina Central", pessoa.nome());

        assertThrows(IllegalArgumentException.class, () -> pessoa.alteraNomePara("x".repeat(256)));
    }
}
