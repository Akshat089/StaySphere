package com.staysphere.property_service.repository;

import com.staysphere.property_service.entity.Property;
import com.staysphere.property_service.enums.PropertyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findByHostId(Long hostId);

    List<Property> findByCity(String city);

    List<Property> findByPropertyType(PropertyType propertyType);

}