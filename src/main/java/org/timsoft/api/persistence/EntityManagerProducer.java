package org.timsoft.api.persistence;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

@ApplicationScoped
public class EntityManagerProducer {

  private EntityManagerFactory emf;

  @PostConstruct
  void startup() {
    emf = Persistence.createEntityManagerFactory("sampledbPU");
  }

  @PreDestroy
  void shutdown() {
    if (emf != null && emf.isOpen()) {
      emf.close();
    }
  }

  @Produces
  @RequestScoped
  public EntityManager createEntityManager() {
    return emf.createEntityManager();
  }

  public void closeEntityManager(@Disposes EntityManager em) {
    if (em.isOpen()) {
      em.close();
    }
  }
}
