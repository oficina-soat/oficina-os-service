package br.com.oficina.os.framework.db;

import br.com.oficina.os.core.entities.pessoa.Pessoa;
import br.com.oficina.os.core.entities.usuario.CpfOperacional;
import br.com.oficina.os.core.entities.usuario.TipoDePapel;
import br.com.oficina.os.core.entities.usuario.Usuario;
import br.com.oficina.os.core.entities.usuario.UsuarioStatus;
import br.com.oficina.os.core.exceptions.UsuarioConflitanteException;
import br.com.oficina.os.core.exceptions.UsuarioNaoEncontradoException;
import br.com.oficina.os.core.interfaces.gateway.UsuarioGateway;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

class InMemoryUsuarioGateway implements UsuarioGateway {
    private final LinkedHashMap<UUID, Usuario> usuarios = new LinkedHashMap<>();

    InMemoryUsuarioGateway() {
        var seedTime = OffsetDateTime.of(2026, 1, 17, 10, 0, 0, 0, ZoneOffset.UTC);
        seed(
                UsuarioStore.SEED_ADMIN_ID,
                "10000000-0000-4000-8000-000000000001",
                "Administrador Laboratorio",
                "84191404067",
                Set.of(TipoDePapel.ADMINISTRATIVO, TipoDePapel.MECANICO, TipoDePapel.RECEPCIONISTA),
                seedTime);
        seed(
                UsuarioStore.SEED_MECANICO_ID,
                "10000000-0000-4000-8000-000000000002",
                "Mecanico Laboratorio",
                "36655462007",
                Set.of(TipoDePapel.MECANICO),
                seedTime);
        seed(
                UsuarioStore.SEED_RECEPCIONISTA_ID,
                "10000000-0000-4000-8000-000000000003",
                "Recepcionista Laboratorio",
                "17245011010",
                Set.of(TipoDePapel.RECEPCIONISTA),
                seedTime);
    }

    @Override
    public synchronized Usuario criar(Usuario usuario) {
        if (usuarios.containsKey(usuario.id())) {
            throw new UsuarioConflitanteException("Usuário operacional já cadastrado: " + usuario.id());
        }
        assegurarDocumentoDisponivel(usuario.pessoa().documento().valor(), null);
        usuarios.put(usuario.id(), usuario);
        return usuario;
    }

    @Override
    public synchronized List<Usuario> listar() {
        return usuarios.values().stream()
                .sorted(Comparator.comparing(Usuario::criadoEm).thenComparing(Usuario::id))
                .toList();
    }

    @Override
    public synchronized Usuario buscar(UUID usuarioId) {
        var usuario = usuarios.get(usuarioId);
        if (usuario == null) {
            throw new UsuarioNaoEncontradoException(usuarioId);
        }
        return usuario;
    }

    @Override
    public synchronized Usuario atualizar(Usuario usuario) {
        buscar(usuario.id());
        assegurarDocumentoDisponivel(usuario.pessoa().documento().valor(), usuario.id());
        usuarios.put(usuario.id(), usuario);
        return usuario;
    }

    @Override
    public synchronized void inativar(UUID usuarioId) {
        buscar(usuarioId).inativar();
    }

    private void seed(
            UUID usuarioId,
            String pessoaId,
            String nome,
            String documento,
            Set<TipoDePapel> papeis,
            OffsetDateTime seedTime) {
        var pessoa = new Pessoa(UUID.fromString(pessoaId), new CpfOperacional(documento), nome);
        usuarios.put(usuarioId, new Usuario(
                usuarioId,
                pessoa,
                UsuarioStatus.ATIVO,
                papeis,
                seedTime,
                seedTime));
    }

    private void assegurarDocumentoDisponivel(String documento, UUID usuarioIgnorado) {
        var duplicado = usuarios.values().stream()
                .filter(usuario -> usuarioIgnorado == null || !usuario.id().equals(usuarioIgnorado))
                .anyMatch(usuario -> usuario.pessoa().documento().valor().equals(documento));
        if (duplicado) {
            throw new UsuarioConflitanteException("CPF já vinculado a um usuário operacional: " + documento);
        }
    }
}
