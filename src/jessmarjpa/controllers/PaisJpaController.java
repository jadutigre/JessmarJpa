/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jessmarjpa.controllers;

import java.io.Serializable;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import jessmarjpa.entities.Estado;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import jessmarjpa.controllers.exceptions.IllegalOrphanException;
import jessmarjpa.controllers.exceptions.NonexistentEntityException;
import jessmarjpa.entities.Pais;

/**
 *
 * @author jadut
 */
public class PaisJpaController implements Serializable {

    public PaisJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Pais pais) {
        if (pais.getEstadoList() == null) {
            pais.setEstadoList(new ArrayList<Estado>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            List<Estado> attachedEstadoList = new ArrayList<Estado>();
            for (Estado estadoListEstadoToAttach : pais.getEstadoList()) {
                estadoListEstadoToAttach = em.getReference(estadoListEstadoToAttach.getClass(), estadoListEstadoToAttach.getId());
                attachedEstadoList.add(estadoListEstadoToAttach);
            }
            pais.setEstadoList(attachedEstadoList);
            em.persist(pais);
            for (Estado estadoListEstado : pais.getEstadoList()) {
                Pais oldPaisIdOfEstadoListEstado = estadoListEstado.getPaisId();
                estadoListEstado.setPaisId(pais);
                estadoListEstado = em.merge(estadoListEstado);
                if (oldPaisIdOfEstadoListEstado != null) {
                    oldPaisIdOfEstadoListEstado.getEstadoList().remove(estadoListEstado);
                    oldPaisIdOfEstadoListEstado = em.merge(oldPaisIdOfEstadoListEstado);
                }
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Pais pais) throws IllegalOrphanException, NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Pais persistentPais = em.find(Pais.class, pais.getId());
            List<Estado> estadoListOld = persistentPais.getEstadoList();
            List<Estado> estadoListNew = pais.getEstadoList();
            List<String> illegalOrphanMessages = null;
            for (Estado estadoListOldEstado : estadoListOld) {
                if (!estadoListNew.contains(estadoListOldEstado)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain Estado " + estadoListOldEstado + " since its paisId field is not nullable.");
                }
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            List<Estado> attachedEstadoListNew = new ArrayList<Estado>();
            for (Estado estadoListNewEstadoToAttach : estadoListNew) {
                estadoListNewEstadoToAttach = em.getReference(estadoListNewEstadoToAttach.getClass(), estadoListNewEstadoToAttach.getId());
                attachedEstadoListNew.add(estadoListNewEstadoToAttach);
            }
            estadoListNew = attachedEstadoListNew;
            pais.setEstadoList(estadoListNew);
            pais = em.merge(pais);
            for (Estado estadoListNewEstado : estadoListNew) {
                if (!estadoListOld.contains(estadoListNewEstado)) {
                    Pais oldPaisIdOfEstadoListNewEstado = estadoListNewEstado.getPaisId();
                    estadoListNewEstado.setPaisId(pais);
                    estadoListNewEstado = em.merge(estadoListNewEstado);
                    if (oldPaisIdOfEstadoListNewEstado != null && !oldPaisIdOfEstadoListNewEstado.equals(pais)) {
                        oldPaisIdOfEstadoListNewEstado.getEstadoList().remove(estadoListNewEstado);
                        oldPaisIdOfEstadoListNewEstado = em.merge(oldPaisIdOfEstadoListNewEstado);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Long id = pais.getId();
                if (findPais(id) == null) {
                    throw new NonexistentEntityException("The pais with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(Long id) throws IllegalOrphanException, NonexistentEntityException {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Pais pais;
            try {
                pais = em.getReference(Pais.class, id);
                pais.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The pais with id " + id + " no longer exists.", enfe);
            }
            List<String> illegalOrphanMessages = null;
            List<Estado> estadoListOrphanCheck = pais.getEstadoList();
            for (Estado estadoListOrphanCheckEstado : estadoListOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This Pais (" + pais + ") cannot be destroyed since the Estado " + estadoListOrphanCheckEstado + " in its estadoList field has a non-nullable paisId field.");
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            em.remove(pais);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Pais> findPaisEntities() {
        return findPaisEntities(true, -1, -1);
    }

    public List<Pais> findPaisEntities(int maxResults, int firstResult) {
        return findPaisEntities(false, maxResults, firstResult);
    }

    private List<Pais> findPaisEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Pais.class));
            Query q = em.createQuery(cq);
            if (!all) {
                q.setMaxResults(maxResults);
                q.setFirstResult(firstResult);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public Pais findPais(Long id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Pais.class, id);
        } finally {
            em.close();
        }
    }

    public int getPaisCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Pais> rt = cq.from(Pais.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
