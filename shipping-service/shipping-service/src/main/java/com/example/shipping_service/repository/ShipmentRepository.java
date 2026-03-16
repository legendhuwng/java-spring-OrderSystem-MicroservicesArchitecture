// ShipmentRepository.java
package com.example.shipping_service.repository;

import com.example.shipping_service.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, String> {}