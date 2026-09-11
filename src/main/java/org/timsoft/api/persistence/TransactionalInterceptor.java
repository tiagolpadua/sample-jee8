package org.timsoft.api.persistence;

@javax.interceptor.Interceptor
@Tx
@javax.annotation.Priority(javax.interceptor.Interceptor.Priority.APPLICATION)
public class TransactionalInterceptor {

  @javax.inject.Inject javax.persistence.EntityManager em;

  @javax.interceptor.AroundInvoke
  public Object runInTransaction(javax.interceptor.InvocationContext ctx) throws Exception {
    javax.persistence.EntityTransaction tx = em.getTransaction();
    boolean owner = !tx.isActive();
    if (owner) {
      tx.begin();
    }
    try {
      Object result = ctx.proceed();
      if (owner) {
        tx.commit();
      }
      return result;
    } catch (RuntimeException e) {
      if (owner && tx.isActive()) {
        tx.rollback();
      }
      throw e;
    }
  }
}
