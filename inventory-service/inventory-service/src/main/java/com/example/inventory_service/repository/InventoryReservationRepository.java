package com.example.inventory_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.inventory_service.entity.InventoryReservation;
import com.example.inventory_service.entity.ReservationStatus;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, String> {

    List<InventoryReservation> findByOrderIdAndStatus(String orderId, ReservationStatus status);
}