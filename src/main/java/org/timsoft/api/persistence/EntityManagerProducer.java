package org.timsoft.api.persistence;

@javax.enterprise.context.ApplicationScoped
public class EntityManagerProducer {

  private javax.persistence.EntityManagerFactory emf;

  @javax.annotation.PostConstruct
  void startup() {
    emf = javax.persistence.Persistence.createEntityManagerFactory("sampledbPU");
  }

  @javax.annotation.PreDestroy
  void shutdown() {
    if (emf != null && emf.isOpen()) {
      emf.close();
    }
  }

  @javax.enterprise.inject.Produces
  @javax.enterprise.context.RequestScoped
  public javax.persistence.EntityManager createEntityManager() {
    return emf.createEntityManager();
  }

  public void closeEntityManager(
      @javax.enterprise.inject.Disposes javax.persistence.EntityManager em) {
    if (em.isOpen()) {
      em.close();
    }
  }
}
