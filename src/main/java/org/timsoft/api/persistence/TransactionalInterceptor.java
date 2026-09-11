package org.timsoft.api.persistence;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

@Interceptor
@Tx
@Priority(Interceptor.Priority.APPLICATION)
public class TransactionalInterceptor {

  @Inject EntityManager em;

  @AroundInvoke
  public Object runInTransaction(InvocationContext ctx) throws Exception {
    EntityTransaction tx = em.getTransaction();
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
