package cn.spider.framework.gateway;
import cn.spider.framework.gateway.api.file.FileHandler;
import cn.spider.framework.gateway.api.function.SpiderServerHandler;
import cn.spider.framework.gateway.config.SpringConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * 网关生命周期管理类
 */
public class GatewayVerticle extends AbstractVerticle {

  private AbstractApplicationContext factory;

  public static Vertx clusterVertx;

  /**
   * 启动
   * @param startPromise
   * @throws Exception
   */
  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    this.clusterVertx = vertx;
    // 启动spring-ioc
    this.factory = new AnnotationConfigApplicationContext(SpringConfig.class);

    HttpServer server = vertx.createHttpServer();
    // 跟ui交互的router
    Router fillRouter = Router.router(vertx);
    // 设置 跨域
    fillRouter.route().handler(CorsHandler.create()
            .addOrigin("*")
            .allowedHeader(" x-www-form-urlencoded, Content-Type,x-requested-with")
            .allowedMethod(HttpMethod.GET)
            .allowedMethod(HttpMethod.POST)
            .allowedMethod(HttpMethod.PUT)
            .allowedMethod(HttpMethod.DELETE));
    server.requestHandler(fillRouter).listen(9674);
    // 给apiRouter添加 对于路径的handler
    FileHandler fileApi = factory.getBean(FileHandler.class);
    fileApi.init(fillRouter);
    // 查询文件的router
    Router staticRouter = Router.router(vertx);
    // 设置跨域
    staticRouter.route().handler(CorsHandler.create()
            .addOrigin("*")
            .allowedHeader(" x-www-form-urlencoded, Content-Type,x-requested-with")
            .allowedMethod(HttpMethod.GET)
            .allowedMethod(HttpMethod.POST)
            .allowedMethod(HttpMethod.PUT)
            .allowedMethod(HttpMethod.DELETE));
    // 该下面文件,通过
    staticRouter.route("/*")
            .handler(StaticHandler.create("file"));
    HttpServer serverStatic = vertx.createHttpServer();
    serverStatic.requestHandler(staticRouter).listen(9675);

    Router apiRouter = Router.router(vertx);

    apiRouter.route().handler(CorsHandler.create()
            .addOrigin("*")
            .allowedHeader(" x-www-form-urlencoded, Content-Type,x-requested-with")
            .allowedMethod(HttpMethod.GET)
            .allowedMethod(HttpMethod.POST)
            .allowedMethod(HttpMethod.PUT)
            .allowedMethod(HttpMethod.DELETE));
    apiRouter.route().handler(BodyHandler.create());
    SpiderServerHandler spiderServerHandler = factory.getBean(SpiderServerHandler.class);
    // 进行handler注册
    spiderServerHandler.init(apiRouter);

    HttpServer apiServer = vertx.createHttpServer();
    apiServer.requestHandler(apiRouter).listen(9677);

    startPromise.complete();
  }

  /**
   * 关闭
   * @param stopPromise
   */
  @Override
  public void stop(Promise<Void> stopPromise){
    stopPromise.complete();
  }
}
