package com.desk.controller;

import com.desk.dto.TicketDTO;
import com.desk.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    // POST /api/tickets
    @PostMapping
    public ResponseEntity<TicketDTO> register(@RequestBody TicketDTO dto) {
        TicketDTO created = ticketService.register(dto);
        return ResponseEntity
                .created(URI.create("/api/tickets/" + created.tno()))
                .body(created);
    }

    // GET /api/tickets/{tno}
    @GetMapping("/{tno}")
    public ResponseEntity<TicketDTO> read(@PathVariable Long tno) {
        return ResponseEntity.ok(ticketService.read(tno));
    }

    // GET /api/tickets
    @GetMapping
    public ResponseEntity<List<TicketDTO>> list() {
        return ResponseEntity.ok(ticketService.list());
    }

    // PUT /api/tickets/{tno}
    @PutMapping("/{tno}")
    public ResponseEntity<TicketDTO> modify(@PathVariable Long tno, @RequestBody TicketDTO dto) {
        return ResponseEntity.ok(ticketService.modify(tno, dto));
    }

    // DELETE /api/tickets/{tno}
    @DeleteMapping("/{tno}")
    public ResponseEntity<Void> remove(@PathVariable Long tno) {
        ticketService.remove(tno);
        return ResponseEntity.noContent().build();
    }
}
