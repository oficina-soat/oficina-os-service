package br.com.oficina.os.interfaces.controllers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.not;

import br.com.oficina.os.framework.db.UsuarioStore;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class UsuariosResourceTest {

    @Test
    @TestSecurity(user = "admin", roles = "administrativo")
    void deveExecutarCrudAdministrativoSemExporCredenciais() {
        var idempotencyKey = "usuario-create-" + UUID.randomUUID();
        var usuarioId = given()
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType("application/json")
                .body("""
                        {
                          "nome": "Ana Operadora",
                          "documento": "52998224725",
                          "papeis": ["mecanico", "recepcionista"]
                        }
                        """)
                .when()
                .post("/api/v1/usuarios")
                .then()
                .statusCode(201)
                .header("Location", notNullValue())
                .body("usuarioId", notNullValue())
                .body("pessoaId", notNullValue())
                .body("nome", equalTo("Ana Operadora"))
                .body("documento", equalTo("52998224725"))
                .body("tipoPessoa", equalTo("FISICA"))
                .body("status", equalTo("ATIVO"))
                .body("papeis", hasItems("mecanico", "recepcionista"))
                .body("criadoEm", notNullValue())
                .body("atualizadoEm", notNullValue())
                .body("$", not(hasKey("password")))
                .body("$", not(hasKey("passwordHash")))
                .extract()
                .path("usuarioId")
                .toString();

        given()
                .queryParam("page", 0)
                .queryParam("size", 2)
                .when()
                .get("/api/v1/usuarios")
                .then()
                .statusCode(200)
                .body("items.size()", equalTo(2))
                .body("totalItems", greaterThanOrEqualTo(4))
                .body("page", equalTo(0))
                .body("size", equalTo(2));

        given()
                .when()
                .get("/api/v1/usuarios/{usuarioId}", usuarioId)
                .then()
                .statusCode(200)
                .body("usuarioId", equalTo(usuarioId));

        given()
                .contentType("application/json")
                .body("""
                        {
                          "nome": "Ana Administradora",
                          "documento": "11144477735",
                          "status": "BLOQUEADO",
                          "papeis": ["administrativo"]
                        }
                        """)
                .when()
                .put("/api/v1/usuarios/{usuarioId}", usuarioId)
                .then()
                .statusCode(200)
                .body("nome", equalTo("Ana Administradora"))
                .body("documento", equalTo("11144477735"))
                .body("status", equalTo("BLOQUEADO"))
                .body("papeis[0]", equalTo("administrativo"));

        given()
                .when()
                .delete("/api/v1/usuarios/{usuarioId}", usuarioId)
                .then()
                .statusCode(204);

        given()
                .when()
                .delete("/api/v1/usuarios/{usuarioId}", usuarioId)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/api/v1/usuarios/{usuarioId}", usuarioId)
                .then()
                .statusCode(200)
                .body("status", equalTo("INATIVO"));
    }

    @Test
    @TestSecurity(user = "admin", roles = "administrativo")
    void deveAplicarValidacaoIdempotenciaConflitoENaoEncontrado() {
        given()
                .header("X-Idempotency-Key", "usuario-sem-corpo-" + UUID.randomUUID())
                .contentType("application/json")
                .body("null")
                .when()
                .post("/api/v1/usuarios")
                .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"));

        given()
                .contentType("application/json")
                .body("""
                        {
                          "nome": "Sem Chave",
                          "documento": "52998224725",
                          "papeis": ["mecanico"]
                        }
                        """)
                .when()
                .post("/api/v1/usuarios")
                .then()
                .statusCode(400)
                .body("code", equalTo("IDEMPOTENCY_KEY_REQUIRED"));

        given()
                .header("X-Idempotency-Key", "usuario-duplicado-" + UUID.randomUUID())
                .contentType("application/json")
                .body("""
                        {
                          "nome": "Administrador Duplicado",
                          "documento": "84191404067",
                          "status": "ATIVO",
                          "papeis": ["administrativo"]
                        }
                        """)
                .when()
                .post("/api/v1/usuarios")
                .then()
                .statusCode(409)
                .body("code", equalTo("DUPLICATE_RESOURCE"));

        given()
                .header("X-Idempotency-Key", "usuario-invalido-" + UUID.randomUUID())
                .contentType("application/json")
                .body("""
                        {
                          "nome": "Usuario Invalido",
                          "documento": "84191404067",
                          "status": "SUSPENSO",
                          "papeis": ["financeiro"]
                        }
                        """)
                .when()
                .post("/api/v1/usuarios")
                .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"));

        given()
                .when()
                .get("/api/v1/usuarios/{usuarioId}", UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("code", equalTo("RESOURCE_NOT_FOUND"));
    }

    @Test
    void deveExigirAutenticacao() {
        given()
                .header("X-Correlation-Id", "usuarios-auth-required")
                .when()
                .get("/api/v1/usuarios")
                .then()
                .statusCode(401)
                .header("X-Correlation-Id", equalTo("usuarios-auth-required"))
                .body("code", equalTo("AUTHENTICATION_REQUIRED"))
                .body("correlationId", equalTo("usuarios-auth-required"));
    }

    @Test
    void deveRejeitarTokenInvalido() {
        given()
                .header("Authorization", "Bearer token-invalido")
                .when()
                .get("/api/v1/usuarios")
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTHENTICATION_INVALID"));
    }

    @Test
    @TestSecurity(user = "mecanico", roles = "mecanico")
    void deveNegarUsuarioSemPapelAdministrativo() {
        given()
                .when()
                .get("/api/v1/usuarios")
                .then()
                .statusCode(403)
                .body("code", equalTo("ACCESS_DENIED"));
    }

    @Test
    @TestSecurity(user = "admin", roles = "administrativo")
    void deveConsultarSeedsOperacionais() {
        given()
                .when()
                .get("/api/v1/usuarios/{usuarioId}", UsuarioStore.SEED_ADMIN_ID)
                .then()
                .statusCode(200)
                .body("nome", equalTo("Administrador Laboratorio"))
                .body("papeis", hasItems("administrativo", "mecanico", "recepcionista"));
    }
}
