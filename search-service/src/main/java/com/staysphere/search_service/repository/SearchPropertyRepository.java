package com.staysphere.search_service.repository;

import com.staysphere.search_service.entity.SearchProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SearchPropertyRepository extends JpaRepository<SearchProperty, Long>,
        JpaSpecificationExecutor<SearchProperty> {

    Optional<SearchProperty> findByPropertyId(Long propertyId);

    boolean existsByPropertyId(Long propertyId);

    void deleteByPropertyId(Long propertyId);
}