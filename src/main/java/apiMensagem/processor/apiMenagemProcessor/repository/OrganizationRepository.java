package apiMensagem.processor.apiMenagemProcessor.repository;

import apiMensagem.processor.apiMenagemProcessor.entity.OrganizationsEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends MongoRepository<OrganizationsEntity, String> {

    Optional<OrganizationsEntity> findByorgId(String orgId);
}
