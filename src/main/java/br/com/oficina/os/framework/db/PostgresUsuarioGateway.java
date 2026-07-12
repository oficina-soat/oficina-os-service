package br.com.oficina.os.framework.db;

import br.com.oficina.os.core.entities.pessoa.Pessoa;
import br.com.oficina.os.core.entities.pessoa.TipoPessoa;
import br.com.oficina.os.core.entities.usuario.CpfOperacional;
import br.com.oficina.os.core.entities.usuario.TipoDePapel;
import br.com.oficina.os.core.entities.usuario.Usuario;
import br.com.oficina.os.core.entities.usuario.UsuarioStatus;
import br.com.oficina.os.core.exceptions.UsuarioConflitanteException;
import br.com.oficina.os.core.exceptions.UsuarioNaoEncontradoException;
import br.com.oficina.os.core.interfaces.gateway.UsuarioGateway;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;

class PostgresUsuarioGateway implements UsuarioGateway {
    private static final String SQL_SELECT_USUARIOS = """
            SELECT u.id AS usuario_id,
                   p.id AS pessoa_id,
                   p.nome,
                   p.documento,
                   p.tipo_pessoa,
                   u.status,
                   u.criado_em,
                   u.atualizado_em,
                   COALESCE(string_agg(up.papel_codigo, ',' ORDER BY up.papel_codigo), '') AS papeis
            FROM usuario u
            JOIN pessoa p ON p.id = u.pessoa_id
            LEFT JOIN usuario_papel up ON up.usuario_id = u.id
            """;
    private static final String SQL_GROUP_USUARIO = """
            GROUP BY u.id, p.id, p.nome, p.documento, p.tipo_pessoa,
                     u.status, u.criado_em, u.atualizado_em
            """;

    private final DataSource dataSource;

    PostgresUsuarioGateway(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Usuario criar(Usuario usuario) {
        return inTransaction(connection -> {
            var pessoa = persistirOuReutilizarPessoa(connection, usuario.pessoa(), usuario.atualizadoEm());
            var persistido = new Usuario(
                    usuario.id(),
                    pessoa,
                    usuario.status(),
                    usuario.papeis(),
                    usuario.criadoEm(),
                    usuario.atualizadoEm());
            try (var statement = connection.prepareStatement("""
                    INSERT INTO usuario (id, pessoa_id, status, criado_em, atualizado_em)
                    VALUES (?, ?, ?, ?, ?)
                    """)) {
                statement.setObject(1, persistido.id());
                statement.setObject(2, persistido.pessoa().id());
                statement.setString(3, persistido.status().name());
                statement.setObject(4, persistido.criadoEm());
                statement.setObject(5, persistido.atualizadoEm());
                statement.executeUpdate();
            }
            substituirPapeis(connection, persistido.id(), persistido.papeis());
            return buscar(connection, persistido.id());
        });
    }

    @Override
    public List<Usuario> listar() {
        var sql = SQL_SELECT_USUARIOS + SQL_GROUP_USUARIO + " ORDER BY u.criado_em, u.id";
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(sql);
                var resultSet = statement.executeQuery()) {
            var resultado = new ArrayList<Usuario>();
            while (resultSet.next()) {
                resultado.add(toUsuario(resultSet));
            }
            return List.copyOf(resultado);
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    @Override
    public Usuario buscar(UUID usuarioId) {
        try (var connection = dataSource.getConnection()) {
            return buscar(connection, usuarioId);
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    @Override
    public Usuario atualizar(Usuario usuario) {
        return inTransaction(connection -> {
            var pessoaId = bloquearUsuario(connection, usuario.id());
            assegurarDocumentoDisponivel(connection, usuario.pessoa().documento().valor(), pessoaId);
            try (var statement = connection.prepareStatement("""
                    UPDATE pessoa
                    SET nome = ?, documento = ?, tipo_pessoa = ?, atualizado_em = ?
                    WHERE id = ?
                    """)) {
                statement.setString(1, usuario.pessoa().nome());
                statement.setString(2, usuario.pessoa().documento().valor());
                statement.setString(3, usuario.pessoa().tipoPessoa().name());
                statement.setObject(4, usuario.atualizadoEm());
                statement.setObject(5, pessoaId);
                statement.executeUpdate();
            }
            try (var statement = connection.prepareStatement("""
                    UPDATE usuario
                    SET status = ?, atualizado_em = ?
                    WHERE id = ?
                    """)) {
                statement.setString(1, usuario.status().name());
                statement.setObject(2, usuario.atualizadoEm());
                statement.setObject(3, usuario.id());
                statement.executeUpdate();
            }
            substituirPapeis(connection, usuario.id(), usuario.papeis());
            return buscar(connection, usuario.id());
        });
    }

    @Override
    public void inativar(UUID usuarioId) {
        inTransaction(connection -> {
            bloquearUsuario(connection, usuarioId);
            try (var statement = connection.prepareStatement("""
                    UPDATE usuario
                    SET status = 'INATIVO', atualizado_em = ?
                    WHERE id = ? AND status <> 'INATIVO'
                    """)) {
                statement.setObject(1, OffsetDateTime.now(ZoneOffset.UTC));
                statement.setObject(2, usuarioId);
                statement.executeUpdate();
            }
            return null;
        });
    }

    private Pessoa persistirOuReutilizarPessoa(
            Connection connection,
            Pessoa pessoa,
            OffsetDateTime atualizadoEm) throws SQLException {
        var pessoaExistenteId = buscarPessoaPorDocumento(connection, pessoa.documento().valor());
        if (pessoaExistenteId == null) {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO pessoa (id, documento, tipo_pessoa, nome, criado_em, atualizado_em)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """)) {
                statement.setObject(1, pessoa.id());
                statement.setString(2, pessoa.documento().valor());
                statement.setString(3, pessoa.tipoPessoa().name());
                statement.setString(4, pessoa.nome());
                statement.setObject(5, atualizadoEm);
                statement.setObject(6, atualizadoEm);
                statement.executeUpdate();
            }
            return pessoa;
        }
        if (existeUsuarioDaPessoa(connection, pessoaExistenteId)) {
            throw new UsuarioConflitanteException(
                    "CPF já vinculado a um usuário operacional: " + pessoa.documento().valor());
        }
        try (var statement = connection.prepareStatement("""
                UPDATE pessoa
                SET nome = ?, tipo_pessoa = ?, atualizado_em = ?
                WHERE id = ?
                """)) {
            statement.setString(1, pessoa.nome());
            statement.setString(2, pessoa.tipoPessoa().name());
            statement.setObject(3, atualizadoEm);
            statement.setObject(4, pessoaExistenteId);
            statement.executeUpdate();
        }
        return new Pessoa(pessoaExistenteId, pessoa.documento(), pessoa.tipoPessoa(), pessoa.nome());
    }

    private UUID buscarPessoaPorDocumento(Connection connection, String documento) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT id FROM pessoa WHERE documento = ? FOR UPDATE")) {
            statement.setString(1, documento);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getObject("id", UUID.class) : null;
            }
        }
    }

    private boolean existeUsuarioDaPessoa(Connection connection, UUID pessoaId) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT 1 FROM usuario WHERE pessoa_id = ?")) {
            statement.setObject(1, pessoaId);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void assegurarDocumentoDisponivel(Connection connection, String documento, UUID pessoaId) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT 1 FROM pessoa WHERE documento = ? AND id <> ?")) {
            statement.setString(1, documento);
            statement.setObject(2, pessoaId);
            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    throw new UsuarioConflitanteException("CPF já vinculado a outra Pessoa: " + documento);
                }
            }
        }
    }

    private UUID bloquearUsuario(Connection connection, UUID usuarioId) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT pessoa_id FROM usuario WHERE id = ? FOR UPDATE")) {
            statement.setObject(1, usuarioId);
            try (var resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new UsuarioNaoEncontradoException(usuarioId);
                }
                return resultSet.getObject("pessoa_id", UUID.class);
            }
        }
    }

    private void substituirPapeis(Connection connection, UUID usuarioId, Set<TipoDePapel> papeis) throws SQLException {
        try (var delete = connection.prepareStatement("DELETE FROM usuario_papel WHERE usuario_id = ?")) {
            delete.setObject(1, usuarioId);
            delete.executeUpdate();
        }
        try (var insert = connection.prepareStatement(
                "INSERT INTO usuario_papel (usuario_id, papel_codigo) VALUES (?, ?)")) {
            for (var papel : papeis) {
                insert.setObject(1, usuarioId);
                insert.setString(2, papel.valor());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private Usuario buscar(Connection connection, UUID usuarioId) throws SQLException {
        var sql = SQL_SELECT_USUARIOS + " WHERE u.id = ? " + SQL_GROUP_USUARIO;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setObject(1, usuarioId);
            try (var resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new UsuarioNaoEncontradoException(usuarioId);
                }
                return toUsuario(resultSet);
            }
        }
    }

    private Usuario toUsuario(ResultSet resultSet) throws SQLException {
        var pessoa = new Pessoa(
                resultSet.getObject("pessoa_id", UUID.class),
                new CpfOperacional(resultSet.getString("documento")),
                TipoPessoa.valueOf(resultSet.getString("tipo_pessoa")),
                resultSet.getString("nome"));
        return new Usuario(
                resultSet.getObject("usuario_id", UUID.class),
                pessoa,
                UsuarioStatus.valueOf(resultSet.getString("status")),
                papeis(resultSet.getString("papeis")),
                offsetDateTime(resultSet, "criado_em"),
                offsetDateTime(resultSet, "atualizado_em"));
    }

    private Set<TipoDePapel> papeis(String raw) {
        var papeis = new LinkedHashSet<TipoDePapel>();
        if (raw != null && !raw.isBlank()) {
            for (var valor : raw.split(",")) {
                papeis.add(TipoDePapel.fromValor(valor));
            }
        }
        return papeis;
    }

    private OffsetDateTime offsetDateTime(ResultSet resultSet, String column) throws SQLException {
        var value = resultSet.getObject(column);
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        }
        throw new SQLException("Coluna " + column + " não pode ser convertida para OffsetDateTime.");
    }

    private RuntimeException persistenceFailure(SQLException exception) {
        if ("23505".equals(exception.getSQLState())) {
            return new UsuarioConflitanteException("CPF já vinculado a outra Pessoa ou Usuário operacional.");
        }
        return new IllegalStateException("Falha ao acessar usuários no PostgreSQL do oficina-os-service.", exception);
    }

    private <T> T inTransaction(SqlOperation<T> operation) {
        try (var connection = dataSource.getConnection()) {
            var previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                var result = operation.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection);
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException _) {
            // A falha original é mais útil para o chamador.
        }
    }

    @FunctionalInterface
    private interface SqlOperation<T> {
        T execute(Connection connection) throws SQLException;
    }
}
