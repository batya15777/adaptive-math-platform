package com.adaptive.server.service;

import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Transactional
@Component
@SuppressWarnings("unchecked")
public class Persist {

    @PersistenceContext
    private EntityManager entityManager;

    public <T> void saveAll(List<T> objects) {
        for (T object : objects) {
            getQuerySession().saveOrUpdate(object);
        }
    }

    public void remove(Object o) {
        entityManager.remove(entityManager.contains(o) ? o : entityManager.merge(o));
    }

    public Session getQuerySession() {
        return entityManager.unwrap(Session.class);
    }

    public void save(Object object) {
        getQuerySession().saveOrUpdate(object);
    }

    public <T> T loadObject(Class<T> clazz, long oid) {
        return getQuerySession().get(clazz, oid);
    }

    public <T> List<T> loadList(Class<T> clazz) {
        return getQuerySession()
                .createQuery("FROM " + clazz.getSimpleName()).list();
    }


}
