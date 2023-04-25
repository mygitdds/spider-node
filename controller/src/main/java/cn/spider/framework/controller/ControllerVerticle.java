package cn.spider.framework.controller;

import cn.spider.framework.controller.config.ControllerConfig;
import cn.spider.framework.controller.election.ElectionLeader;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import java.util.ArrayList;
import java.util.List;

public class ControllerVerticle extends AbstractVerticle {

  private AbstractApplicationContext factory;

  public static Vertx clusterVertx;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    this.clusterVertx = vertx;
    this.factory = new AnnotationConfigApplicationContext(ControllerConfig.class);
    ElectionLeader electionLeader = this.factory.getBean(ElectionLeader.class);
    // 进行选举
    electionLeader.election();

    startPromise.complete();
  }

  /**
   * 关闭verticle
   *
   * @param stopPromise
   */
  @Override
  public void stop(Promise<Void> stopPromise) {
    stopPromise.complete();
    factory.close();
  }
}
