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
import jessmarjpa.entities.Pais;
import jessmarjpa.entities.Clientes;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import jessmarjpa.controllers.exceptions.IllegalOrphanException;
import jessmarjpa.controllers.exceptions.NonexistentEntityException;
import jessmarjpa.entities.Estado;

/**
 *
 * @author jadut
 */
public class EstadoJpaController implements Serializable {

    public EstadoJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Estado estado) {
        if (estado.getClientesList() == null) {
            estado.setClientesList(new ArrayList<Clientes>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Pais paisId = estado.getPaisId();
            if (paisId != null) {
                paisId = em.getReference(paisId.getClass(), paisId.getId());
                estado.setPaisId(paisId);
            }
            List<Clientes> attachedClientesList = new ArrayList<Clientes>();
            for (Clientes clientesListClientesToAttach : estado.getClientesList()) {
                clientesListClientesToAttach = em.getReference(clientesListClientesToAttach.getClass(), clientesListClientesToAttach.getId());
                attachedClientesList.add(clientesListClientesToAttach);
            }
            estado.setClientesList(attachedClientesList);
            em.persist(estado);
            if (paisId != null) {
                paisId.getEstadoList().add(estado);
                paisId = em.merge(paisId);
            }
            for (Clientes clientesListClientes : estado.getClientesList()) {
                Estado oldEstadoIdOfClientesListClientes = clientesListClientes.getEstadoId();
                clientesListClientes.setEstadoId(estado);
                clientesListClientes = em.merge(clientesListClientes);
                if (oldEstadoIdOfClientesListClientes != null) {
                    oldEstadoIdOfClientesListClientes.getClientesList().remove(clientesListClientes);
                    oldEstadoIdOfClientesListClientes = em.merge(oldEstadoIdOfClientesListClientes);
                }
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Estado estado) throws IllegalOrphanException, NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Estado persistentEstado = em.find(Estado.class, estado.getId());
            Pais paisIdOld = persistentEstado.getPaisId();
            Pais paisIdNew = estado.getPaisId();
            List<Clientes> clientesListOld = persistentEstado.getClientesList();
            List<Clientes> clientesListNew = estado.getClientesList();
            List<String> illegalOrphanMessages = null;
            for (Clientes clientesListOldClientes : clientesListOld) {
                if (!clientesListNew.contains(clientesListOldClientes)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain Clientes " + clientesListOldClientes + " since its estadoId field is not nullable.");
                }
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            if (paisIdNew != null) {
                paisIdNew = em.getReference(paisIdNew.getClass(), paisIdNew.getId());
                estado.setPaisId(paisIdNew);
            }
            List<Clientes> attachedClientesListNew = new ArrayList<Clientes>();
            for (Clientes clientesListNewClientesToAttach : clientesListNew) {
                clientesListNewClientesToAttach = em.getReference(clientesListNewClientesToAttach.getClass(), clientesListNewClientesToAttach.getId());
                attachedClientesListNew.add(clientesListNewClientesToAttach);
            }
            clientesListNew = attachedClientesListNew;
            estado.setClientesList(clientesListNew);
            estado = em.merge(estado);
            if (paisIdOld != null && !paisIdOld.equals(paisIdNew)) {
                paisIdOld.getEstadoList().remove(estado);
                paisIdOld = em.merge(paisIdOld);
            }
            if (paisIdNew != null && !paisIdNew.equals(paisIdOld)) {
                paisIdNew.getEstadoList().add(estado);
                paisIdNew = em.merge(paisIdNew);
            }
            for (Clientes clientesListNewClientes : clientesListNew) {
                if (!clientesListOld.contains(clientesListNewClientes)) {
                    Estado oldEstadoIdOfClientesListNewClientes = clientesListNewClientes.getEstadoId();
                    clientesListNewClientes.setEstadoId(estado);
                    clientesListNewClientes = em.merge(clientesListNewClientes);
                    if (oldEstadoIdOfClientesListNewClientes != null && !oldEstadoIdOfClientesListNewClientes.equals(estado)) {
                        oldEstadoIdOfClientesListNewClientes.getClientesList().remove(clientesListNewClientes);
                        oldEstadoIdOfClientesListNewClientes = em.merge(oldEstadoIdOfClientesListNewClientes);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Long id = estado.getId();
                if (findEstado(id) == null) {
                    throw new NonexistentEntityException("The estado with id " + id + " no longer exists.");
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
            Estado estado;
            try {
                estado = em.getReference(Estado.class, id);
                estado.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The estado with id " + id + " no longer exists.", enfe);
            }
            List<String> illegalOrphanMessages = null;
            List<Clientes> clientesListOrphanCheck = estado.getClientesList();
            for (Clientes clientesListOrphanCheckClientes : clientesListOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This Estado (" + estado + ") cannot be destroyed since the Clientes " + clientesListOrphanCheckClientes + " in its clientesList field has a non-nullable estadoId field.");
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            Pais paisId = estado.getPaisId();
            if (paisId != null) {
                paisId.getEstadoList().remove(estado);
                paisId = em.merge(paisId);
            }
            em.remove(estado);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Estado> findEstadoEntities() {
        return findEstadoEntities(true, -1, -1);
    }

    public List<Estado> findEstadoEntities(int maxResults, int firstResult) {
        return findEstadoEntities(false, maxResults, firstResult);
    }

    private List<Estado> findEstadoEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Estado.class));
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

    public Estado findEstado(Long id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Estado.class, id);
        } finally {
            em.close();
        }
    }

    public int getEstadoCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Estado> rt = cq.from(Estado.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
