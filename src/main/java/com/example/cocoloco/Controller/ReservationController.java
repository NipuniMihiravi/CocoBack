package com.example.cocoloco.Controller;

import com.example.cocoloco.Model.Reservation;
import com.example.cocoloco.Service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/reservation")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    // Get all reservations
    @GetMapping
    public List<Reservation> getAllReservations() {
        return reservationService.getAllReservations();
    }

    // Get a single reservation by ID
    @GetMapping("/{id}")
    public ResponseEntity<Reservation> getReservationById(@PathVariable String id) {
        return reservationService.getReservationById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Add a new reservation
    @PostMapping
    public ResponseEntity<String> addReservation(@RequestBody Reservation reservation) {
        // Fetch existing reservations for the given date
        List<Reservation> existingReservations = reservationService.getReservationsByDate(reservation.getReservationDate());

        // Check if "full" is already reserved for this date
        boolean isFullReserved = existingReservations.stream()
                .anyMatch(existingReservation -> existingReservation.getTimeSlot().equalsIgnoreCase("full"));

        // Prevent booking "day" or "night" if "full" is reserved
        if (isFullReserved && (reservation.getTimeSlot().equalsIgnoreCase("day") || reservation.getTimeSlot().equalsIgnoreCase("night"))) {
            return ResponseEntity.badRequest().body("Error: The 'full' time slot is already reserved.");
        }

        // Check if "day" or "night" reservations exist
        boolean isDayBooked = existingReservations.stream()
                .anyMatch(existingReservation -> existingReservation.getTimeSlot().equalsIgnoreCase("day"));
        boolean isNightBooked = existingReservations.stream()
                .anyMatch(existingReservation -> existingReservation.getTimeSlot().equalsIgnoreCase("night"));

        // Prevent booking "full" if either "day" or "night" is booked
        if (reservation.getTimeSlot().equalsIgnoreCase("full") && (isDayBooked || isNightBooked)) {
            return ResponseEntity.badRequest().body("Error: 'Day' or 'Night' time slot is already reserved, so 'full' cannot be booked.");
        }

        // Prevent booking the same time slot again
        boolean isTimeSlotAlreadyBooked = existingReservations.stream()
                .anyMatch(existingReservation -> existingReservation.getTimeSlot().equalsIgnoreCase(reservation.getTimeSlot()));

        if (isTimeSlotAlreadyBooked) {
            return ResponseEntity.badRequest().body("Error: This time slot is already reserved.");
        }

        // If the time slot is available, proceed to add the reservation
        Reservation newReservation = reservationService.addReservation(reservation);
        return ResponseEntity.ok("Reservation successfully added with ID: " + newReservation.getId());
    }


    // Update a reservation by ID
    @PutMapping("/{id}")
    public ResponseEntity<Reservation> updateReservation(@PathVariable String id, @RequestBody Reservation updatedReservation) {
        Reservation reservation = reservationService.updateReservation(id, updatedReservation);
        return reservation != null ? ResponseEntity.ok(reservation) : ResponseEntity.notFound().build();
    }

    // Delete a reservation by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable String id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }

    // Get reservations by date
    @GetMapping("/date/{reservationDate}")
    public List<Reservation> getReservationsByDate(@PathVariable String reservationDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date = LocalDate.parse(reservationDate, formatter);
        return reservationService.getReservationsByDate(date);
    }

    // Check availability for a specific date
    @GetMapping("/checkAvailability")
    public ResponseEntity<String> checkAvailability(@RequestParam String reservationDate) {
        // Parse the reservation date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date = LocalDate.parse(reservationDate, formatter);

        // Fetch reservations for the date
        List<Reservation> reservations = reservationService.getReservationsByDate(date);

        // Check if the full day is booked
        boolean isFullDayBooked = reservations.stream()
                .anyMatch(reservation -> reservation.getTimeSlot().equalsIgnoreCase("full"));
        if (isFullDayBooked) {
            return ResponseEntity.ok("full");
        }

        // Check individual time slots (day, night)
        boolean isDayBooked = reservations.stream()
                .anyMatch(reservation -> reservation.getTimeSlot().equalsIgnoreCase("day"));
        boolean isNightBooked = reservations.stream()
                .anyMatch(reservation -> reservation.getTimeSlot().equalsIgnoreCase("night"));

        if (isDayBooked && isNightBooked) {
            return ResponseEntity.ok("full");
        } else if (isDayBooked) {
            return ResponseEntity.ok("night");
        } else if (isNightBooked) {
            return ResponseEntity.ok("day");
        } else {
            return ResponseEntity.ok("day, night");
        }
    }
}
