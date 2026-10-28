package com.gympabon.controller;

import com.gympabon.controller.dto.TrainerRequest;
import com.gympabon.controller.dto.TrainerResponse;
import com.gympabon.model.Trainer;
import com.gympabon.service.TrainerService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trainer endpoints. Anyone signed in can list them (to choose a trainer for a
 * Premium membership); only the administrator can change them.
 */
@RestController
@RequestMapping("/api/trainers")
public class TrainerController {

    private final TrainerService trainerService;
    private final SessionManager sessionManager;

    public TrainerController(TrainerService trainerService, SessionManager sessionManager) {
        this.trainerService = trainerService;
        this.sessionManager = sessionManager;
    }

    /** GET /api/trainers */
    @GetMapping
    public List<TrainerResponse> list(HttpSession session) {
        sessionManager.requireUser(session);
        return trainerService.listTrainers().stream().map(TrainerResponse::from).toList();
    }

    /** GET /api/trainers/available: active trainers with at least one free slot. */
    @GetMapping("/available")
    public List<TrainerResponse> available(HttpSession session) {
        sessionManager.requireUser(session);
        return trainerService.findAvailableTrainers().stream().map(TrainerResponse::from).toList();
    }

    /** GET /api/trainers/{id} */
    @GetMapping("/{id}")
    public TrainerResponse findById(@PathVariable("id") int id, HttpSession session) {
        sessionManager.requireUser(session);
        return TrainerResponse.from(trainerService.findById(id));
    }

    /** POST /api/trainers (administrator) */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrainerResponse register(@RequestBody TrainerRequest request, HttpSession session) {
        sessionManager.requireAdmin(session);
        return TrainerResponse.from(trainerService.registerTrainer(request.toTrainer()));
    }

    /** PUT /api/trainers/{id} (administrator) */
    @PutMapping("/{id}")
    public TrainerResponse update(@PathVariable("id") int id, @RequestBody TrainerRequest request,
                                  HttpSession session) {
        sessionManager.requireAdmin(session);
        Trainer trainer = request.toTrainer();
        trainer.setId(id);
        trainerService.updateTrainer(trainer);
        return TrainerResponse.from(trainerService.findById(id));
    }

    /** PATCH /api/trainers/{id}/deactivate (administrator) */
    @PatchMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable("id") int id, HttpSession session) {
        sessionManager.requireAdmin(session);
        trainerService.deactivateTrainer(id);
    }
}
