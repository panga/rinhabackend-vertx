package dev.panga.rinhabackend;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.*;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.UUID;

public class MainVerticle extends AbstractVerticle {

  private SqlClient client;

  public static void main(String[] args) {
    Vertx.vertx().deployVerticle(new MainVerticle());
  }

  @Override
  public void start() {
    PgConnectOptions connectOptions = new PgConnectOptions()
      .setPort(5432)
      .setHost("db")
      .setDatabase("rinhadb")
      .setUser("root")
      .setPassword("1234");
    PoolOptions poolOptions = new PoolOptions().setMaxSize(4);
    this.client = PgPool.client(vertx, connectOptions, poolOptions);

    final Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.route().failureHandler(this::handleFailure);
    router.post("/pessoas").handler(this::criarPessoa);
    router.get("/pessoas/:id").handler(this::consultarPessoa);
    router.get("/pessoas").handler(this::buscarPessoas);
    router.get("/contagem-pessoas").handler(this::contarPessoas);

    vertx.createHttpServer().requestHandler(router).listen(80, r -> {
      System.out.println(String.format("Listening on port %d", r.result().actualPort()));
    });
  }

  private void handleFailure(RoutingContext routingContext) {
    // TODO: add proper error handling
    routingContext.response().setStatusCode(routingContext.statusCode()).end();
  }

  private void criarPessoa(RoutingContext routingContext) {
    String apelido, nome, nascimento;
    String[] stack;

    // Syntax validation
    try {
      JsonObject payload = routingContext.getBodyAsJson();
      apelido = payload.getString("apelido");
      nome = payload.getString("nome");
      nascimento = payload.getString("nascimento");
      JsonArray stackArray = payload.getJsonArray("stack");
      if (stackArray != null) {
        stack = stackArray.stream().toArray(String[]::new);
      } else {
        stack = null;
      }
    } catch (Exception e) {
      routingContext.fail(400, e);
      return;
    }

    // Content validation
    if (apelido != null) {
      if (apelido.length() > 32) {
        routingContext.fail(422);
        return;
      }
    } else {
      routingContext.fail(422);
      return;
    }

    if (nome != null) {
      if (nome.length() > 100) {
        routingContext.fail(422);
        return;
      }
    } else {
      routingContext.fail(422);
      return;
    }

    if (nascimento != null) {
        try {
          new SimpleDateFormat("yyyy-MM-dd").parse(nascimento);
        } catch (Exception e) {
          routingContext.fail(422, e);
          return;
        }
    } else {
      routingContext.fail(422);
      return;
    }

    if (stack != null) {
      for (String s : stack) {
        if (s.length() > 32) {
          routingContext.fail(422);
          return;
        }
      }
    }

    // Insert data
    String id = UUID.randomUUID().toString();
    StringBuilder buscaText = new StringBuilder();
    buscaText.append(apelido).append(" ").append(nome);
    if (stack != null) {
      buscaText.append(" ").append(String.join(" ", stack));
    }

    Tuple insertValues = Tuple.of(id, apelido, nome, nascimento);
    insertValues.addArrayOfString(stack);
    insertValues.addString(buscaText.toString().toLowerCase());

    client
      .preparedQuery("INSERT INTO PESSOAS (ID, APELIDO, NOME, NASCIMENTO, STACK, BUSCA_TRGM)\n" +
        "VALUES ($1, $2, $3, $4, $5, $6)")
      .execute(insertValues)
      .onComplete(ar -> {
        if (ar.succeeded()) {
          routingContext.response()
            .setStatusCode(201)
            .putHeader("Location", "/pessoas/" + id)
            .end();
        } else {
          routingContext.fail(422, ar.cause());
        }
      });
  }

  private void consultarPessoa(RoutingContext routingContext) {
    String id = routingContext.pathParam("id");
    if (id == null) {
      routingContext.fail(400);
      return;
    }

    client
      .preparedQuery("SELECT ID, APELIDO, NOME, NASCIMENTO, STACK\n" +
        "FROM PESSOAS\n" +
        "WHERE ID = $1")
      .execute(Tuple.of(id))
      .onComplete(ar -> {
        if (ar.succeeded()) {
          RowSet<Row> rows = ar.result();
          if (rows.size() > 0) {
            JsonObject pessoa = rows.iterator().next().toJson();
            routingContext.json(pessoa);
          } else {
            routingContext.fail(404);
          }
        } else {
          routingContext.fail(500, ar.cause());
        }
      });
  }

  private void buscarPessoas(RoutingContext routingContext) {
    List<String> params = routingContext.queryParam("t");
    String termo = params != null && !params.isEmpty() ? params.get(0) : null;
    if (termo == null) {
      routingContext.fail(400);
      return;
    }

    client
      .preparedQuery("SELECT ID, APELIDO, NOME, NASCIMENTO, STACK\n" +
        "FROM PESSOAS\n" +
        "WHERE BUSCA_TRGM LIKE $1\n" +
        "LIMIT 50")
      .execute(Tuple.of("%" + termo.toLowerCase() + "%"))
      .onComplete(ar -> {
        if (ar.succeeded()) {
          RowSet<Row> rows = ar.result();
          JsonArray pessoas = new JsonArray();
          for (Row row : rows) {
            JsonObject pessoa = row.toJson();
            pessoas.add(pessoa);
          }
          routingContext.json(pessoas);
        } else {
          routingContext.fail(500, ar.cause());
        }
      });
  }

  private void contarPessoas(RoutingContext routingContext) {
    client
      .preparedQuery("SELECT COUNT(1) FROM PESSOAS")
      .execute()
      .onComplete(ar -> {
        if (ar.succeeded()) {
          RowSet<Row> rows = ar.result();
          Integer count = rows.iterator().next().getInteger(0);
          routingContext.end(count.toString());
        } else {
          routingContext.fail(500, ar.cause());
        }
      });
  }
}
