package com.gympabon.controller;

import com.gympabon.controller.dto.ClientRequest;
import com.gympabon.controller.dto.ClientResponse;
import com.gympabon.model.Client;
import com.gympabon.service.ClientService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Client endpoints, used by the administrator and the reception.
 */
@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;
    private final SessionManager sessionManager;

    public ClientController(ClientService clientService, SessionManager sessionManager) {
        this.clientService = clientService;
        this.sessionManager = sessionManager;
    }

    /** GET /api/clients?q=text: searches by document or name; without text, lists all. */
    @GetMapping
    public List<ClientResponse> search(@RequestParam(name = "q", required = false) String text,
                                       HttpSession session) {
        sessionManager.requireUser(session);
        return clientService.searchClients(text).stream().map(ClientResponse::from).toList();
    }

    /** GET /api/clients/{id} */
    @GetMapping("/{id}")
    public ClientResponse findById(@PathVariable("id") int id, HttpSession session) {
        sessionManager.requireUser(session);
        return ClientResponse.from(clientService.findById(id));
    }

    /** GET /api/clients/document/{documentNumber} */
    @GetMapping("/document/{documentNumber}")
    public ClientResponse findByDocument(@PathVariable("documentNumber") String documentNumber,
                                         HttpSession session) {
        sessionManager.requireUser(session);
        return ClientResponse.from(clientService.findByDocument(documentNumber));
    }

    /** POST /api/clients: registers a new client. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse register(@RequestBody ClientRequest request, HttpSession session) {
        sessionManager.requireUser(session);
        return ClientResponse.from(clientService.registerClient(request.toClient()));
    }

    /** PUT /api/clients/{id}: updates the data of a client. */
    @PutMapping("/{id}")
    public ClientResponse update(@PathVariable("id") int id, @RequestBody ClientRequest request,
                                 HttpSession session) {
        sessionManager.requireUser(session);
        Client client = request.toClient();
        client.setId(id);
        clientService.updateClient(client);
        return ClientResponse.from(clientService.findById(id));
    }

    /** PATCH /api/clients/{id}/deactivate */
    @PatchMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable("id") int id, HttpSession session) {
        sessionManager.requireUser(session);
        clientService.deactivateClient(id);
    }
}
